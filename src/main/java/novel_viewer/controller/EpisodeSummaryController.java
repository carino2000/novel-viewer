package novel_viewer.controller;

import lombok.RequiredArgsConstructor;
import novel_viewer.domain.repository.EpisodeSummaryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/summary")
public class EpisodeSummaryController {

    private final EpisodeSummaryRepository episodeSummaryRepository;

    @GetMapping
    public ResponseEntity<?> get(@RequestParam Long novelId, @RequestParam Integer episodeNumber) {
        return episodeSummaryRepository.findByNovel_NovelIdAndEpisodeNumber(novelId, episodeNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(null));
    }
}
