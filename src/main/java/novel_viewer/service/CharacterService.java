package novel_viewer.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import novel_viewer.domain.entity.Character;
import novel_viewer.domain.repository.CharacterRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final ObjectMapper objectMapper;

    public Map<String, List<Character>> search(Long novelId, String keyword) {
        String lower = keyword.toLowerCase();
        List<Character> all = characterRepository.findByNovel_NovelId(novelId);

        List<Character> byName = new ArrayList<>();
        List<Character> byClass = new ArrayList<>();
        List<Character> byUnique = new ArrayList<>();
        List<Character> bySpecial = new ArrayList<>();
        List<Character> byLatent = new ArrayList<>();

        for (Character c : all) {
            if (c.getName() != null && c.getName().toLowerCase().contains(lower)) {
                byName.add(c);
            }
            if (c.getAbilitiesJson() != null) {
                try {
                    AbilitiesNode ab = objectMapper.readValue(c.getAbilitiesJson(), AbilitiesNode.class);
                    if (ab.className() != null && ab.className().toLowerCase().contains(lower)) {
                        byClass.add(c);
                    }
                    if (containsKeyword(ab.unique(), lower)) byUnique.add(c);
                    if (containsKeyword(ab.special(), lower)) bySpecial.add(c);
                    if (containsKeyword(ab.latent(), lower)) byLatent.add(c);
                } catch (Exception ignored) {}
            }
        }

        Map<String, List<Character>> result = new LinkedHashMap<>();
        if (!byName.isEmpty())   result.put("이름", byName);
        if (!byClass.isEmpty())  result.put("클래스", byClass);
        if (!byUnique.isEmpty()) result.put("고유능력", byUnique);
        if (!bySpecial.isEmpty()) result.put("특수능력", bySpecial);
        if (!byLatent.isEmpty()) result.put("잠재능력", byLatent);
        return result;
    }

    private boolean containsKeyword(List<AbilityNode> list, String keyword) {
        if (list == null) return false;
        return list.stream().anyMatch(a -> a.name() != null && a.name().toLowerCase().contains(keyword));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AbilitiesNode(
            String className,
            @JsonProperty("고유") List<AbilityNode> unique,
            @JsonProperty("특수") List<AbilityNode> special,
            @JsonProperty("잠재") List<AbilityNode> latent
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AbilityNode(String name) {}
}
