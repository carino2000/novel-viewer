package novel_viewer.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reading_progress")
public class ReadingProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long progressId;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id")
    private Novel novel;
    private Integer currentEpisode;
    private Integer paragraphIndex;
    private Integer lastUpdatedEpisode;
    private LocalDateTime lastReadAt;
    @Getter(onMethod_ = {@JsonRawValue, @JsonProperty("relations")})
    @Column(columnDefinition = "JSON")
    private String relationsJson;

    @PrePersist
    public void prePersist() {
        this.lastReadAt = LocalDateTime.now();
        if (this.currentEpisode == null) this.currentEpisode = 1;
        if (this.paragraphIndex == null) this.paragraphIndex = 0;
        if (this.lastUpdatedEpisode == null) this.lastUpdatedEpisode = 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.lastReadAt = LocalDateTime.now();
    }
}
