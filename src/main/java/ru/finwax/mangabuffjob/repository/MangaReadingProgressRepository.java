package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.finwax.mangabuffjob.Entity.MangaReadingProgress;

import java.util.Optional;

@Repository
public interface MangaReadingProgressRepository extends JpaRepository<MangaReadingProgress, Long> {
    Optional<MangaReadingProgress> findByMangaIdAndUserCookieId(Long mangaId, Long userId);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE MANGA_READING_PROGRESS
    SET chapter_readed = :chapterReaded,
        has_readed = :hasReaded,
        last_updated = CURRENT_TIMESTAMP
    WHERE manga_id = :mangaId AND user_id = :userId;
    
    INSERT INTO MANGA_READING_PROGRESS(manga_id, user_id, chapter_readed, has_readed, last_updated)
    SELECT :mangaId, :userId, :chapterReaded, :hasReaded, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
        SELECT 1 FROM MANGA_READING_PROGRESS
        WHERE manga_id = :mangaId AND user_id = :userId
    );
    """, nativeQuery = true)
    void upsertProgress(@Param("mangaId") Long mangaId,
                        @Param("userId") Long userId,
                        @Param("chapterReaded") Integer chapterReaded,
                        @Param("hasReaded") Boolean hasReaded);

    Optional<MangaReadingProgress> findMangaReadingProgressByUserCookieIdAndHasReadedIsFalse(Long userId);

    @Query(value = """
        SELECT * FROM manga_reading_progress
        WHERE has_readed=true and user_id=:userId
        ORDER BY manga_id DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<MangaReadingProgress> findMangaProgress(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM MangaReadingProgress m WHERE m.userCookie.id = :userId")
    void deleteByUserCookieId(@Param("userId") Long userId);
}
