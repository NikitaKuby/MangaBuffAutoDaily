package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ru.finwax.mangabuffjob.Entity.MangaReadingProgress;

import java.util.Optional;

public interface MangaReadingProgressRepository extends JpaRepository<MangaReadingProgress, Long> {
    Optional<MangaReadingProgress> findByMangaId(Long mangaId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO manga_reading_progress
        (manga_id, chapter_readed, has_readed, last_updated)
        VALUES (:mangaId, :chapterReaded, :hasReaded, NOW())
        ON CONFLICT (manga_id)
        DO UPDATE SET
            chapter_readed = EXCLUDED.chapter_readed,
            has_readed = EXCLUDED.has_readed,
            last_updated = EXCLUDED.last_updated
        """, nativeQuery = true)
    void upsertProgress(Long mangaId, Integer chapterReaded, Boolean hasReaded);
}
