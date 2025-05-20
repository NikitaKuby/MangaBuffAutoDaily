package ru.finwax.mangabuffjob.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Builder.Default
    @Column(name = "adv_done", nullable = false)
    private Integer advDone = 0;

    @Column(name = "last_updated", nullable = false)
    private LocalDate lastUpdated;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "avatar_alt_text")
    private String avatarAltText;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

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