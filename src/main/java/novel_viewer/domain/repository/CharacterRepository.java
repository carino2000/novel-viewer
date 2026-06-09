package novel_viewer.domain.repository;

import novel_viewer.domain.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CharacterRepository extends JpaRepository<Character, Long> {
    List<Character> findByNovel_NovelId(Long novelId);
    Optional<Character> findByNovel_NovelIdAndName(Long novelId, String name);
}
