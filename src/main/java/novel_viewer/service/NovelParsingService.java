package novel_viewer.service;

import lombok.RequiredArgsConstructor;
import novel_viewer.domain.entity.Episode;
import novel_viewer.domain.entity.Novel;
import novel_viewer.domain.repository.EpisodeRepository;
import novel_viewer.domain.repository.NovelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NovelParsingService {

    private static final String NOVEL_FILE_PATH = "C:\\Users\\carin\\Desktop\\메모라이즈_UTF8.txt";

    // 00001  제목  ====...====
    private static final Pattern EPISODE_HEADER = Pattern.compile("^(\\d{5})\\s{2}(.+?)\\s{2}=+\\s*$");

    private final NovelRepository novelRepository;
    private final EpisodeRepository episodeRepository;

    @Transactional
    public int parseAndSave() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(NOVEL_FILE_PATH), StandardCharsets.UTF_8);

        List<Episode> episodes = new ArrayList<>();
        int currentEpisodeNumber = -1;
        String currentTitle = null;
        StringBuilder contentBuilder = new StringBuilder();

        for (String line : lines) {
            Matcher matcher = EPISODE_HEADER.matcher(line);
            if (matcher.matches()) {
                if (currentEpisodeNumber > 0) {
                    episodes.add(buildEpisode(null, currentEpisodeNumber, currentTitle, contentBuilder.toString().trim()));
                }
                currentEpisodeNumber = Integer.parseInt(matcher.group(1));
                currentTitle = matcher.group(2).trim();
                contentBuilder = new StringBuilder();
            } else if (currentEpisodeNumber > 0) {
                contentBuilder.append(line).append("\n");
            }
        }
        if (currentEpisodeNumber > 0) {
            episodes.add(buildEpisode(null, currentEpisodeNumber, currentTitle, contentBuilder.toString().trim()));
        }

        Novel novel = novelRepository.save(Novel.builder()
                .title(extractTitle(NOVEL_FILE_PATH))
                .totalEpisodes(episodes.size())
                .build());

        episodes.forEach(e -> e.setNovel(novel));
        episodeRepository.saveAll(episodes);

        return episodes.size();
    }

    private Episode buildEpisode(Novel novel, int episodeNumber, String title, String content) {
        return Episode.builder()
                .novel(novel)
                .episodeNumber(episodeNumber)
                .title(title)
                .content(content)
                .charCount(content.length())
                .build();
    }

    private String extractTitle(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        return fileName.replaceAll("_UTF8\\.txt$", "").replaceAll("\\.txt$", "");
    }
}
