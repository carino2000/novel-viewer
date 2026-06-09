package novel_viewer.domain.repository;

import novel_viewer.domain.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    List<Episode> findByNovel_NovelId(Long novelId);
    Optional<Episode> findByNovel_NovelIdAndEpisodeNumber(Long novelId, Integer episodeNumber);
    Optional<Episode> findByEpisodeNumber(Integer episodeNumber);
}
