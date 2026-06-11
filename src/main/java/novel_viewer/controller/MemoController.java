package novel_viewer.controller;

import lombok.RequiredArgsConstructor;
import novel_viewer.domain.entity.Memo;
import novel_viewer.domain.entity.Novel;
import novel_viewer.domain.repository.MemoRepository;
import novel_viewer.domain.repository.NovelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memo")
public class MemoController {

    private final MemoRepository memoRepository;
    private final NovelRepository novelRepository;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam Long novelId) {
        return ResponseEntity.ok(memoRepository.findByNovel_NovelId(novelId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest req) {
        Novel novel = novelRepository.findById(req.novelId())
                .orElseThrow(() -> new IllegalArgumentException("소설을 찾을 수 없습니다: " + req.novelId()));
        Memo memo = Memo.builder()
                .novel(novel)
                .title(req.title())
                .content(req.content())
                .episodeNumber(req.episodeNumber())
                .build();
        return ResponseEntity.ok(memoRepository.save(memo));
    }

    @PutMapping("/{memoId}")
    public ResponseEntity<?> update(@PathVariable Long memoId, @RequestBody UpdateRequest req) {
        return memoRepository.findById(memoId)
                .map(memo -> {
                    if (req.title() != null) memo.setTitle(req.title());
                    if (req.content() != null) memo.setContent(req.content());
                    return ResponseEntity.ok(memoRepository.save(memo));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{memoId}")
    public ResponseEntity<?> delete(@PathVariable Long memoId) {
        if (!memoRepository.existsById(memoId)) {
            return ResponseEntity.notFound().build();
        }
        memoRepository.deleteById(memoId);
        return ResponseEntity.ok().build();
    }

    public record CreateRequest(Long novelId, String title, String content, Integer episodeNumber) {}
    public record UpdateRequest(String title, String content) {}
}
