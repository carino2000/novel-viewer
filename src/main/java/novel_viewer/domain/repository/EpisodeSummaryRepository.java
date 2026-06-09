package novel_viewer.domain.repository;

import novel_viewer.domain.entity.EpisodeSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EpisodeSummaryRepository extends JpaRepository<EpisodeSummary, Long> {
    List<EpisodeSummary> findByNovel_NovelId(Long novelId);
    Optional<EpisodeSummary> findByNovel_NovelIdAndEpisodeNumber(Long novelId, Integer episodeNumber);
}
