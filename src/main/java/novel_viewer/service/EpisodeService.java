package novel_viewer.service;

import lombok.RequiredArgsConstructor;
import novel_viewer.domain.entity.Episode;
import novel_viewer.domain.repository.EpisodeRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final EpisodeRepository episodeRepository;

    public Episode getEpisode(Long novelId, int episodeNumber) {
        return episodeRepository.findByNovel_NovelIdAndEpisodeNumber(novelId, episodeNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차입니다: " + episodeNumber));
    }
}
