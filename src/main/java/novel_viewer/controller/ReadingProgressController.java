package novel_viewer.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;
import novel_viewer.domain.entity.ReadingProgress;
import novel_viewer.service.ReadingProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reading-progress")
public class ReadingProgressController {

    private final ReadingProgressService readingProgressService;

    @GetMapping
    public ResponseEntity<?> get(@RequestParam Long novelId) {
        try {
            return ResponseEntity.ok(readingProgressService.getOrCreate(novelId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody UpdateRequest req) {
        try {
            return ResponseEntity.ok(readingProgressService.update(req.novelId(), req.currentEpisode(), req.paragraphIndex()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record UpdateRequest(Long novelId, Integer currentEpisode, Integer paragraphIndex) {}
}
