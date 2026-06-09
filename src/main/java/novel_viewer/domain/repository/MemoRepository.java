package novel_viewer.domain.repository;

import novel_viewer.domain.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    List<Memo> findByNovel_NovelId(Long novelId);
}
