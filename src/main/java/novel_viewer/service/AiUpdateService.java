package novel_viewer.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import novel_viewer.domain.entity.Character;
import novel_viewer.domain.entity.*;
import novel_viewer.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiUpdateService {

    private final AnthropicClient anthropicClient;
    private final NovelRepository novelRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final EpisodeRepository episodeRepository;
    private final CharacterRepository characterRepository;
    private final EpisodeSummaryRepository episodeSummaryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiUpdateResult update(Long novelId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("소설을 찾을 수 없습니다: " + novelId));

        ReadingProgress progress = readingProgressRepository.findByNovel_NovelId(novelId)
                .orElseThrow(() -> new IllegalStateException("읽기 진행상황이 없습니다. 먼저 읽기를 시작해주세요."));

        int fromEpisode = progress.getLastUpdatedEpisode() + 1;
        int toEpisode = progress.getCurrentEpisode();

        if (fromEpisode > toEpisode) {
            throw new IllegalStateException(
                    "새로 읽은 에피소드가 없습니다. (마지막 업데이트: " + progress.getLastUpdatedEpisode() + "화)");
        }

        List<Episode> episodes = episodeRepository
                .findByNovel_NovelIdAndEpisodeNumberBetweenOrderByEpisodeNumberAsc(novelId, fromEpisode, toEpisode);

        if (episodes.isEmpty()) {
            throw new IllegalStateException(
                    "해당 범위의 에피소드를 찾을 수 없습니다 (" + fromEpisode + "~" + toEpisode + "화).");
        }

        List<Character> existingCharacters = characterRepository.findByNovel_NovelId(novelId);
        int paragraphIndex = progress.getParagraphIndex() != null ? progress.getParagraphIndex() : 0;

        String rawResponse = callClaude(novel, episodes, existingCharacters, fromEpisode, toEpisode, paragraphIndex);
        AiResponseJson response = parseResponse(rawResponse);

        saveCharacters(novel, response.characters(), toEpisode);
        saveSummary(novel, response.summary(), toEpisode);
        updateProgress(progress, response.relations(), toEpisode);

        return new AiUpdateResult(
                response.characters() != null ? response.characters().size() : 0,
                response.summary(),
                response.relations() != null ? response.relations().size() : 0,
                fromEpisode,
                toEpisode
        );
    }

    // ── 시스템 프롬프트: 역할 + 출력 형식 정의 (캐시 대상) ──────────────────
    private String buildSystemPrompt() {
        return """
                당신은 한국 웹소설 분석 전문가입니다.
                사용자가 제공하는 에피소드 원문을 분석하고, 아래 JSON 형식으로만 응답합니다.
                설명, 인사, 부연 텍스트 없이 JSON 코드블록만 반환하세요.

                이 소설은 헌터·이능력자 장르로, 캐릭터들이 게임 시스템의 상태창(스탯·능력)을 보유합니다.

                ## 출력 형식

                ```json
                {
                  "characters": [
                    {
                      "name": "캐릭터명",
                      "description": "외형·성격·능력·역할을 3~5문장으로 서술",
                      "firstAppearedAt": 처음등장화수(정수),
                      "stats": {
                        "근력": 0,
                        "내구": 0,
                        "민첩": 0,
                        "체력": 0,
                        "마력": 0,
                        "행운": 0
                      },
                      "abilities": {
                        "className": "클래스명 (없으면 null)",
                        "alignment": "성향 (없으면 null)",
                        "고유": [{"name": "능력명", "rank": "랭크"}],
                        "특수": [{"name": "능력명", "rank": "랭크"}],
                        "잠재": [{"name": "능력명", "rank": "랭크"}]
                      }
                    }
                  ],
                  "summary": "이번 분석 범위의 핵심 사건과 흐름을 2~3문단으로 요약",
                  "relations": [
                    {"from": "인물A", "to": "인물B", "relation": "관계 유형 (15자 이내)"}
                  ]
                }
                ```

                ## 작성 규칙

                ### characters
                - 이름이 명시된 주요 인물만 포함. 단역·무명 NPC 제외.
                - description: 원문에서 확인된 사실만 기술. 추측 금지.
                - stats: 원문에 수치(숫자)가 명시된 경우만 입력. 묘사만 있고 수치가 없으면 0.
                - abilities.className: 원문에 클래스가 명시된 경우만 입력.
                - abilities.alignment: 성향이 명시된 경우만 입력 (예: "질서·혼돈", "혼돈·악" 등).
                - abilities.고유/특수/잠재: 원문의 상태창에 나타난 능력만 포함. rank는 원문 그대로 (예: "S", "A Plus", "EX").
                - 능력 정보가 전혀 없는 캐릭터는 abilities를 null로.
                - firstAppearedAt: 이전 회차 등장 인물은 기존 화수 유지.
                - 기존 인물 정보가 제공되면, 변경·추가된 내용만 반영.

                ### summary
                - 다음 회차를 이어 읽기 위한 복기용 요약.
                - 주요 전투·사건·반전·인물 변화를 중심으로 작성.
                - 감상이나 평가 없이 사실 위주로.

                ### relations
                - 주요 인물 간 관계만 포함. 단방향, 중복 없음.
                - 예시: "동료", "라이벌", "적대", "사제지간", "계약 관계", "연인" 등
                """;
    }

    // ── 유저 메시지: 소설 컨텍스트 + 기존 인물 + 에피소드 원문 ──────────────
    private String buildUserMessage(Novel novel, List<Episode> episodes,
                                    List<Character> existingChars, int from, int to, int paragraphIndex) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 소설: ").append(novel.getTitle())
          .append("  |  분석 범위: ").append(from).append("화 ~ ").append(to).append("화\n\n");

        if (!existingChars.isEmpty()) {
            sb.append("---\n## 현재 등록된 인물 (이전 AI 업데이트 기준)\n\n");
            for (Character c : existingChars) {
                sb.append("- **").append(c.getName()).append("** (").append(c.getFirstAppearedAt()).append("화 첫 등장)\n");
                sb.append("  ").append(c.getDescription()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("---\n## 에피소드 원문\n\n");
        for (int i = 0; i < episodes.size(); i++) {
            Episode ep = episodes.get(i);
            boolean isLastEpisode = (i == episodes.size() - 1);

            sb.append("### ").append(ep.getEpisodeNumber()).append("화. ").append(ep.getTitle()).append("\n\n");

            // 마지막 화는 사용자가 읽은 문단까지만 전송
            if (isLastEpisode && paragraphIndex > 0) {
                String[] lines = ep.getContent().split("\n");
                int cutoff = Math.min(paragraphIndex, lines.length);
                sb.append(String.join("\n", Arrays.copyOfRange(lines, 0, cutoff)));
                sb.append("\n\n*(").append(cutoff).append("/").append(lines.length).append(" 줄까지 읽음)*\n\n");
            } else {
                sb.append(ep.getContent()).append("\n\n");
            }
        }

        return sb.toString();
    }

    // ── Claude API 호출 (스트리밍 + 프롬프트 캐싱) ──────────────────────────
    private String callClaude(Novel novel, List<Episode> episodes,
                               List<Character> existingChars, int from, int to, int paragraphIndex) {
        String systemPrompt = buildSystemPrompt();
        String userMessage = buildUserMessage(novel, episodes, existingChars, from, to, paragraphIndex);

        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-opus-4-8")
                .maxTokens(8192L)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.HIGH)
                        .build())
                // 시스템 프롬프트를 캐시 블록으로 전송 (1시간 캐시 → 반복 호출 시 토큰 비용 90% 절감)
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(systemPrompt)
                                .cacheControl(CacheControlEphemeral.builder()
                                        .ttl(CacheControlEphemeral.Ttl.TTL_1H)
                                        .build())
                                .build()
                ))
                .addUserMessage(userMessage)
                .build();

        StringBuilder fullResponse = new StringBuilder();

        try (StreamResponse<RawMessageStreamEvent> stream = anthropicClient.messages().createStreaming(params)) {
            stream.stream()
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .flatMap(delta -> delta.delta().text().stream())
                    .forEach(text -> fullResponse.append(text.text()));
        }

        String result = fullResponse.toString().trim();
        log.debug("AI 원본 응답 ({}자):\n{}", result.length(), result.length() > 500 ? result.substring(0, 500) + "..." : result);
        return result;
    }

    // ── 응답 JSON 파싱 ────────────────────────────────────────────────────────
    private AiResponseJson parseResponse(String raw) {
        String json = raw;

        if (json.contains("```json")) {
            int start = json.indexOf("```json") + 7;
            int end = json.lastIndexOf("```");
            if (end > start) json = json.substring(start, end).trim();
        } else if (json.contains("```")) {
            int start = json.indexOf("```") + 3;
            int end = json.lastIndexOf("```");
            if (end > start) json = json.substring(start, end).trim();
        }

        if (!json.trim().startsWith("{")) {
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) json = json.substring(start, end + 1);
        }

        try {
            return objectMapper.readValue(json, AiResponseJson.class);
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패. 원본 응답:\n{}", raw);
            throw new RuntimeException("AI 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    // ── DB 저장 ───────────────────────────────────────────────────────────────
    private void saveCharacters(Novel novel, List<CharacterInfoJson> characters, int currentEpisode) {
        if (characters == null) return;
        for (CharacterInfoJson info : characters) {
            characterRepository.findByNovel_NovelIdAndName(novel.getNovelId(), info.name())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setDescription(info.description());
                                existing.setLastUpdatedAt(currentEpisode);
                                existing.setStatsJson(statsToJson(info.stats()));
                                existing.setAbilitiesJson(abilitiesToJson(info.abilities()));
                                characterRepository.save(existing);
                            },
                            () -> characterRepository.save(
                                    Character.builder()
                                            .novel(novel)
                                            .name(info.name())
                                            .description(info.description())
                                            .firstAppearedAt(info.firstAppearedAt() > 0 ? info.firstAppearedAt() : currentEpisode)
                                            .lastUpdatedAt(currentEpisode)
                                            .statsJson(statsToJson(info.stats()))
                                            .abilitiesJson(abilitiesToJson(info.abilities()))
                                            .build()
                            )
                    );
        }
    }

    private String statsToJson(Map<String, Integer> stats) {
        if (stats == null || stats.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(stats);
        } catch (Exception e) {
            return null;
        }
    }

    private String abilitiesToJson(AbilitiesJson abilities) {
        if (abilities == null) return null;
        try {
            return objectMapper.writeValueAsString(abilities);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveSummary(Novel novel, String summaryText, int currentEpisode) {
        if (summaryText == null || summaryText.isBlank()) return;
        episodeSummaryRepository.findByNovel_NovelIdAndEpisodeNumber(novel.getNovelId(), currentEpisode)
                .ifPresentOrElse(
                        existing -> {
                            existing.setSummaryText(summaryText);
                            episodeSummaryRepository.save(existing);
                        },
                        () -> episodeSummaryRepository.save(
                                EpisodeSummary.builder()
                                        .novel(novel)
                                        .episodeNumber(currentEpisode)
                                        .summaryText(summaryText)
                                        .build()
                        )
                );
    }

    private void updateProgress(ReadingProgress progress, List<RelationInfoJson> relations, int currentEpisode) {
        progress.setLastUpdatedEpisode(currentEpisode);
        if (relations != null && !relations.isEmpty()) {
            try {
                progress.setRelationsJson(objectMapper.writeValueAsString(relations));
            } catch (Exception e) {
                log.warn("관계도 JSON 저장 실패", e);
            }
        }
        readingProgressRepository.save(progress);
    }

    // ── 내부 레코드 ───────────────────────────────────────────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiResponseJson(
            List<CharacterInfoJson> characters,
            String summary,
            List<RelationInfoJson> relations
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CharacterInfoJson(
            String name,
            String description,
            int firstAppearedAt,
            Map<String, Integer> stats,
            AbilitiesJson abilities
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilitiesJson(
            String className,
            String alignment,
            @JsonProperty("고유") List<AbilityInfo> unique,
            @JsonProperty("특수") List<AbilityInfo> special,
            @JsonProperty("잠재") List<AbilityInfo> latent
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilityInfo(
            String name,
            String rank
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelationInfoJson(
            String from,
            String to,
            String relation
    ) {}

    public record AiUpdateResult(
            int characterCount,
            String summary,
            int relationCount,
            int fromEpisode,
            int toEpisode
    ) {}
}
