# Novel Viewer 프로젝트

## 프로젝트 개요
웹소설을 읽으면서 AI 사이드패널을 함께 보여주는 서비스.
밀리의서재처럼 소설을 읽게 해주는 웹앱이며, 오른쪽 패널에서 AI 기능을 제공한다.

## 기술 스택
- **Backend**: Spring Boot (Java 21, Maven)
- **Frontend**: Next.js
- **Database**: MySQL (DB명: novel_viewer, 인코딩: utf8mb4)
- **AI**: Claude API (platform.claude.com 별도 결제)
- **개발환경**: 로컬 PC (배포 없음)
- **로그인 기능**: 미구현 (추후 추가 예정)

## UI 구조
- 화면 왼쪽: 소설 본문 뷰어
- 화면 오른쪽: 탭 패널
  - 인물 정보 탭 (AI 업데이트)
  - 직전 에피소드 요약 탭 (AI 업데이트)
  - 자유 메모장 탭
  - 인물 관계도 탭 (relations_json 기반)

## AI 호출 설계
- 트리거: 사용자가 수동으로 누르는 "AI 업데이트 버튼"
- 전송 범위: last_updated_episode ~ current_episode 구간 원문
- 1회 호출로 인물정보 + 요약 + 관계도를 JSON으로 한번에 받아옴
- 직전 요약은 직전 몇 화 원문을 러프하게 전송
- 챗봇 기능은 복잡도로 인해 제외 (추후 RAG 구조로 구현 고려)

## DB 테이블 구조

### novels (소설 메타정보)
- novel_id, title, author, description, total_episodes, created_at, updated_at

### episodes (화별 원문)
- episode_id, novel_id, episode_number, title, content(LONGTEXT), char_count, created_at
- UNIQUE: (novel_id, episode_number)

### reading_progress (읽기 진행상황)
- progress_id, novel_id, current_episode, paragraph_index, last_updated_episode, relations_json, last_read_at
- UNIQUE: (novel_id)
- paragraph_index: 화 내 문단 위치 (스크롤 복귀용)
- last_updated_episode: 마지막 AI 업데이트 시점 화번호
- relations_json: 인물 관계도 JSON [{"from": "김수현", "to": "세라프", "relation": "적대적 협력"}]

### characters (인물 정보)
- character_id, novel_id, name, description, first_appeared_at, last_updated_at, stats_json, created_at, updated_at
- stats_json 구조: {"근력": 86, "내구": 92, "민첩": 96, "체력": 78, "마력": 48, "행운": 36}
- UNIQUE: (novel_id, name)

### episode_summaries (직전 에피소드 요약 캐시)
- summary_id, novel_id, episode_number, summary_text, created_at
- UNIQUE: (novel_id, episode_number)

### memos (자유 메모장)
- memo_id, novel_id, title(NULL 허용), content, episode_number(NULL 허용), created_at, updated_at

## 소설 파싱 정보
- 현재 소설: 메모라이즈 (파일: `C:\Users\carin\Desktop\메모라이즈_UTF8.txt`)
- 파일 인코딩: UTF-8
- 화 구분 패턴: `^(\d{5})\s{2}(.+?)\s{2}=+\s*$` — 그룹1: 화번호, 그룹2: 제목
- 화당 평균 글자수: 약 7,700자

## 백엔드 패키지 구조
```
novel_viewer/
├── controller/
│   ├── NovelController.java   — 파싱, 삭제
│   └── EpisodeController.java — 회차 조회
├── service/
│   ├── NovelParsingService.java — txt 파싱 & DB 저장
│   └── EpisodeService.java      — 회차 조회
└── domain/
    ├── entity/   — Novel, Episode, ReadingProgress, Character, EpisodeSummary, Memo
    └── repository/ — 각 엔티티별 JpaRepository
```

## JPA 엔티티 규칙
- `novel_viewer.domain.entity` 패키지, 엔티티명 단수형
- FK 관계는 `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = "컬럼명")`
- JSON 직렬화 시 `@ManyToOne` 필드에 `@JsonIgnore` 적용 (LazyInitializationException 방지)
- camelCase → snake_case 자동변환 (Spring Boot 기본 설정)
- `@PrePersist`, `@PreUpdate`로 `createdAt`, `updatedAt` 자동 관리

## API 엔드포인트
| Method | URL | 설명 |
|---|---|---|
| GET | `/api/novel` | 소설 목록 조회 |
| POST | `/api/novel/parse` | txt 파일 파싱 후 Novel+Episode 전체 저장 |
| DELETE | `/api/novel/{novelId}` | 소설 삭제 (cascade로 하위 데이터 전부 삭제) |
| GET | `/api/episode?novelId=1&episodeNumber=1` | 특정 회차 조회 |
| GET | `/api/reading-progress?novelId=1` | 읽기 진행상황 조회 (없으면 1화로 생성) |
| PUT | `/api/reading-progress` | 읽기 진행상황 저장 `{novelId, currentEpisode, paragraphIndex}` |
| POST | `/api/ai/update?novelId=1` | AI 업데이트 (인물/요약/관계도 한번에) |
| GET | `/api/character?novelId=1` | 인물 목록 조회 |
| GET | `/api/summary?novelId=1&episodeNumber=5` | 에피소드 요약 조회 |
| GET | `/api/memo?novelId=1` | 메모 목록 조회 |
| POST | `/api/memo` | 메모 생성 `{novelId, title, content, episodeNumber}` |
| PUT | `/api/memo/{memoId}` | 메모 수정 `{title, content}` |
| DELETE | `/api/memo/{memoId}` | 메모 삭제 |

## 다음 작업 순서
1. ~~JPA 엔티티 전체 작성~~ (완료)
2. ~~소설 파싱 & DB 적재 API 구현~~ (완료)
3. ~~Claude API 연동 & 나머지 API 전체 구현~~ (완료)
4. 프론트엔드 연동 (mock 데이터 → 실제 API 교체)

---

## 작업 일지

### 2026-06-09
- JPA 엔티티 5개 신규 생성: `Episode`, `ReadingProgress`, `Character`, `EpisodeSummary`, `Memo`
- 모든 `novel_id` FK를 `@ManyToOne(fetch = FetchType.LAZY)`으로 설정
- Repository 6개 생성 (각 엔티티 + 주요 쿼리 메서드 포함)
- `NovelParsingService` 구현: 상수로 고정된 txt 파일 경로에서 읽어 회차별 파싱 후 Novel + Episode 일괄 저장
- `EpisodeService` 구현: novelId + episodeNumber로 특정 회차 조회
- `NovelController` 구현: 파싱(`POST /parse`), 소설 삭제(`DELETE /{novelId}`)
- `EpisodeController` 구현: 회차 조회(`GET /api/episode`)

### 2026-06-10 (2차)
- `CorsConfig` 수정: `localhost:3000` 추가 (Vite 포트 불일치 문제 해결)
- `AnthropicConfig`에 `@Bean ObjectMapper` 추가 (AiUpdateService 의존성 오류 수정)
- `AiUpdateService` 프롬프트 고도화: 인물 스탯(근력/내구/민첩 등) + 능력(고유/특수/잠재) 게임 스탯창 형식으로 받도록 수정
- `AiUpdateService` `paragraphIndex` 적용: 마지막 화를 읽은 문단 위치까지만 원문 전송 (`Arrays.copyOfRange`)
- `Character` 엔티티에 `abilitiesJson` 컬럼 추가 (`ALTER TABLE characters ADD COLUMN abilities_json JSON`)
- `Character`, `ReadingProgress` 엔티티에 `@Getter(onMethod_ = {@JsonRawValue, @JsonProperty(...)})` 적용 (stats/abilities/relations JSON 직렬화 수정)
- 프론트엔드 API 연동: `src/api.js`, `App.jsx`, `NovelViewer.jsx`, `SidePanel.jsx`, 각 탭 컴포넌트 모두 실제 API로 교체
- `NovelViewer.jsx` 스크롤 저장: `scrollIntoView` 옵션에 `block: 'start'` 추가 (복원 위치 정확도 개선)
- AI 업데이트 동작 확인: 메모라이즈 9화까지 김수현 스탯/능력 정상 저장 검증

### 2026-06-10
- `anthropic-java:2.34.0` 의존성 추가 (pom.xml)
- `AnthropicConfig`: AnthropicClient Spring Bean 등록 (환경변수 `ANTHROPIC_API_KEY` 사용)
- `CorsConfig`: 전역 CORS 설정 (localhost:5173 허용)
- `AiUpdateService`: Claude Opus 4.8 + 스트리밍 + adaptive thinking으로 인물정보/요약/관계도 한번에 받아 DB 저장
  - 범위: `lastUpdatedEpisode+1` ~ `currentEpisode` 구간 원문 전송
  - 응답 JSON → `characters` upsert, `episode_summaries` 저장, `reading_progress` 업데이트
- `AiController`: `POST /api/ai/update?novelId={id}`
- `ReadingProgressService` + `ReadingProgressController`: `GET/PUT /api/reading-progress`
- `NovelController`: `GET /api/novel` 목록 조회 추가
- `CharacterController`: `GET /api/character?novelId={id}`
- `EpisodeSummaryController`: `GET /api/summary?novelId={id}&episodeNumber={n}`
- `MemoController`: `GET/POST /api/memo`, `PUT/DELETE /api/memo/{id}`
- 엔티티 4개(`Character`, `EpisodeSummary`, `ReadingProgress`, `Memo`)에 `@JsonIgnore` 추가
- `EpisodeRepository`에 범위 조회 메서드 추가: `findByNovel_NovelIdAndEpisodeNumberBetweenOrderByEpisodeNumberAsc`
