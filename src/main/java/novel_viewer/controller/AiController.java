package novel_viewer.controller;

import lombok.RequiredArgsConstructor;
import novel_viewer.service.AiUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {

    private final AiUpdateService aiUpdateService;

    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestParam Long novelId) {
        try {
            AiUpdateService.AiUpdateResult result = aiUpdateService.update(novelId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "AI 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
