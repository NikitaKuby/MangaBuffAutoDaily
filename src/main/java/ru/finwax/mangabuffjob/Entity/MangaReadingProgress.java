package ru.finwax.mangabuffjob.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "manga_reading_progress")
@IdClass(MangaReadingProgressId.class)
public class MangaReadingProgress {
    @Id
    @ManyToOne
    @JoinColumn(name = "manga_id", nullable = false)
    private MangaData manga;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserCookie userCookie;

    @Column(name = "chapter_readed")
    private Integer chapterReaded;

    @Column(name = "has_readed")
    private Boolean hasReaded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}