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
@Table(name = "characters")
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long characterId;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id")
    private Novel novel;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private Integer firstAppearedAt;
    private Integer lastUpdatedAt;
    @Getter(onMethod_ = {@JsonRawValue, @JsonProperty("stats")})
    @Column(columnDefinition = "JSON")
    private String statsJson;
    @Getter(onMethod_ = {@JsonRawValue, @JsonProperty("abilities")})
    @Column(columnDefinition = "JSON")
    private String abilitiesJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.firstAppearedAt == null) this.firstAppearedAt = 0;
        if (this.lastUpdatedAt == null) this.lastUpdatedAt = 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
