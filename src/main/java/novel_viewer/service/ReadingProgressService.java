package novel_viewer.service;

import lombok.RequiredArgsConstructor;
import novel_viewer.domain.entity.Novel;
import novel_viewer.domain.entity.ReadingProgress;
import novel_viewer.domain.repository.NovelRepository;
import novel_viewer.domain.repository.ReadingProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReadingProgressService {

    private final ReadingProgressRepository readingProgressRepository;
    private final NovelRepository novelRepository;

    @Transactional
    public ReadingProgress getOrCreate(Long novelId) {
        return readingProgressRepository.findByNovel_NovelId(novelId)
                .orElseGet(() -> {
                    Novel novel = novelRepository.findById(novelId)
                            .orElseThrow(() -> new IllegalArgumentException("소설을 찾을 수 없습니다: " + novelId));
                    ReadingProgress progress = ReadingProgress.builder()
                            .novel(novel)
                            .currentEpisode(1)
                            .paragraphIndex(0)
                            .lastUpdatedEpisode(0)
                            .build();
                    return readingProgressRepository.save(progress);
                });
    }

    @Transactional
    public ReadingProgress update(Long novelId, Integer currentEpisode, Integer paragraphIndex) {
        ReadingProgress progress = getOrCreate(novelId);
        if (currentEpisode != null) progress.setCurrentEpisode(currentEpisode);
        if (paragraphIndex != null) progress.setParagraphIndex(paragraphIndex);
        return readingProgressRepository.save(progress);
    }
}
