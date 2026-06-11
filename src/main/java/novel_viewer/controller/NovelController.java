package novel_viewer.controller;

import lombok.RequiredArgsConstructor;
import novel_viewer.domain.repository.NovelRepository;
import novel_viewer.service.NovelParsingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/novel")
public class NovelController {

    private final NovelParsingService novelParsingService;
    private final NovelRepository novelRepository;

    @GetMapping
    public ResponseEntity<?> listNovels() {
        return ResponseEntity.ok(novelRepository.findAll());
    }

    @PostMapping("/parse")
    public ResponseEntity<?> newNovelParsing() {
        try {
            int count = novelParsingService.parseAndSave();
            return ResponseEntity.ok(Map.of("savedEpisodes", count));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "파일을 읽을 수 없습니다: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{novelId}")
    public ResponseEntity<?> deleteNovel(@PathVariable Long novelId) {
        if (!novelRepository.existsById(novelId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "존재하지 않는 소설입니다: " + novelId));
        }
        novelRepository.deleteById(novelId);
        return ResponseEntity.ok().build();
    }
}
