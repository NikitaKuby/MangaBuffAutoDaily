package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.finwax.mangabuffjob.Entity.MangaReadingProgress;

import java.util.Optional;

public interface MangaReadingProgressRepository extends JpaRepository<MangaReadingProgress, Long> {
    Optional<MangaReadingProgress> findByMangaIdAndUserCookieId(Long mangaId, Long userId);

    @Modifying
    @Query(value = """
        INSERT INTO manga_reading_progress
        (manga_id, user_id, chapter_readed, has_readed, last_updated)
        VALUES (:mangaId, :userId, :chapterReaded, :hasReaded, NOW())
        ON CONFLICT (manga_id)
        DO UPDATE SET
            chapter_readed = EXCLUDED.chapter_readed,
            has_readed = EXCLUDED.has_readed,
            last_updated = EXCLUDED.last_updated
        """, nativeQuery = true)
    void upsertProgress(@Param("mangaId") Long mangaId,
                        @Param("userId") Long userId,
                        @Param("chapterReaded") Integer chapterReaded,
                        @Param("hasReaded") Boolean hasReaded);
}
