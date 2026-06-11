package novel_viewer.controller;

import lombok.RequiredArgsConstructor;
import novel_viewer.domain.repository.CharacterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/character")
public class CharacterController {

    private final CharacterRepository characterRepository;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam Long novelId) {
        return ResponseEntity.ok(characterRepository.findByNovel_NovelId(novelId));
    }

    @GetMapping("/detail")
    public ResponseEntity<?> detail(@RequestParam Long novelId, @RequestParam String name) {
        return characterRepository.findByNovel_NovelIdAndName(novelId, name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
