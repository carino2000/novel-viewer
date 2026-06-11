package novel_viewer.controller;

import lombok.RequiredArgsConstructor;
import novel_viewer.domain.entity.Episode;
import novel_viewer.domain.repository.NovelRepository;
import novel_viewer.service.EpisodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/episode")
public class EpisodeController {

    private final EpisodeService episodeService;
    private final NovelRepository novelRepository;

    @GetMapping
    public ResponseEntity<?> getEpisode(
            @RequestParam Long novelId,
            @RequestParam int episodeNumber
    ) {
        try {
            Episode episode = episodeService.getEpisode(novelId, episodeNumber);
            int totalEpisodes = novelRepository.findById(novelId)
                    .map(n -> n.getTotalEpisodes() != null ? n.getTotalEpisodes() : episodeNumber)
                    .orElse(episodeNumber);
            return ResponseEntity.ok(Map.of(
                    "episode", episode,
                    "hasPrev", episodeNumber > 1,
                    "hasNext", episodeNumber < totalEpisodes
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
