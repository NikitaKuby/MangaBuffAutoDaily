package ru.finwax.mangabuffjob.Entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "manga_reading_progress")
public class MangaReadingProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "manga_id", referencedColumnName = "id")
    private MangaData manga;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserCookie userCookie;

    @Column(name = "chapter_readed")
    private Integer chapterReaded;

    @Column(name = "has_readed")
    private Boolean hasReaded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}