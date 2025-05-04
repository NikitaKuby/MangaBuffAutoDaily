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

    @Column(name = "reader_done", nullable = false)
    private Integer readerDone = 0;

    @Column(name = "comment_done", nullable = false)
    private Integer commentDone = 0;

    @Builder.Default
    @Column(name = "quiz_done", nullable = false)
    private Boolean quizDone = false;

    @Builder.Default
    @Column(name = "mine_done", nullable = false)
    private Boolean mineDone = false;

    @Builder.Default
    @Column(name = "adv_done", nullable = false)
    private Boolean advDone = false;

    @Column(name = "last_updated", nullable = false)
    private LocalDate lastUpdated;

    @PreUpdate
    @PrePersist
    public void updateLastUpdated() {
        this.lastUpdated = LocalDate.now();
    }
}