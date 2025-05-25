package ru.finwax.mangabuffjob.Entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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

    @ManyToOne
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