package ru.finwax.mangabuffjob.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mangabuff_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MangaProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Builder.Default
    @Column(name = "reader_done", nullable = false)
    private Integer readerDone = 0;

    @Column(name = "total_reader_chapters")
    private Integer totalReaderChapters;

    @Builder.Default
    @Column(name = "comment_done", nullable = false)
    private Integer commentDone = 0;

    @Column(name = "total_comment_chapters")
    private Integer totalCommentChapters;

    @Builder.Default
    @Column(name = "quiz_done", nullable = false)
    private Boolean quizDone = false;

    @Builder.Default
    @Column(name = "mine_hits_left")
    private Integer mineHitsLeft = 100;

    @Column(name = "mine_count_coin")
    private Integer mineCountCoin;

    @Column(name = "mine_lvl")
    private Integer mineLvl;

    @Builder.Default
    @Column(name = "adv_done", nullable = false)
    private Integer advDone = 0;

    @Column(name = "last_updated", nullable = false)
    private LocalDate lastUpdated;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "avatar_alt_text")
    private String avatarAltText;

    @Column(name = "diamond")
    private Long diamond;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private boolean readerEnabled = true;
    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private boolean commentEnabled = false;
    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private boolean quizEnabled = true;
    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private boolean mineEnabled = true;
    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private boolean advEnabled = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "auto_upgrade_enabled", columnDefinition = "boolean default false")
    private boolean autoUpgradeEnabled = false;

    @Builder.Default
    @Column(name = "auto_exchange_enabled", columnDefinition = "boolean default false")
    private boolean autoExchangeEnabled = false;

    @Builder.Default
    @Column(name = "display_order", nullable = true)
    private Integer displayOrder = 0;

    @Column(name = "card_count")
    private Long countCard;

    @PreUpdate
    public void updateLastUpdated() {
        this.lastUpdated = LocalDate.now();
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // updatedAt = LocalDateTime.now(); // updatedAt устанавливается в @PreUpdate
    }
}