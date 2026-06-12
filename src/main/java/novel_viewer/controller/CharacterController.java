package novel_viewer.controller;

import lombok.RequiredArgsConstructor;
import novel_viewer.domain.repository.CharacterRepository;
import novel_viewer.service.CharacterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/character")
public class CharacterController {

    private final CharacterRepository characterRepository;
    private final CharacterService characterService;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam Long novelId) {
        return ResponseEntity.ok(characterRepository.findByNovel_NovelId(novelId));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam Long novelId, @RequestParam String keyword) {
        return ResponseEntity.ok(characterService.search(novelId, keyword));
    }
}
