package novel_viewer.domain.repository;

import novel_viewer.domain.entity.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {
    Optional<ReadingProgress> findByNovel_NovelId(Long novelId);
}
