package novel_viewer.controller;

import lombok.RequiredArgsConstructor;
import novel_viewer.domain.entity.Episode;
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

    @GetMapping
    public ResponseEntity<?> getEpisode(
            @RequestParam Long novelId,
            @RequestParam int episodeNumber
    ) {
        try {
            Episode episode = episodeService.getEpisode(novelId, episodeNumber);
            return ResponseEntity.ok(episode);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
