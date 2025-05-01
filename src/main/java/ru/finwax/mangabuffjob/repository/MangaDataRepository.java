package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.finwax.mangabuffjob.Entity.MangaData;

import java.util.Optional;

@Repository
public interface MangaDataRepository extends JpaRepository<MangaData, Long> {

    boolean existsByUrl(String url);

    Optional<MangaData> findFirstByOrderByIdAsc();

    @Query(value = """
    SELECT m FROM MangaData m
    LEFT JOIN MangaReadingProgress p ON m.id = p.manga.id AND p.userCookie.id = :userId
    WHERE (p IS NULL OR p.hasReaded = false)
    AND (p IS NULL OR p.chapterReaded < m.countChapters)
    ORDER BY 
        CASE WHEN p IS NULL THEN 0 ELSE p.chapterReaded END ASC,
        m.id ASC
    LIMIT 1
    """)
    Optional<MangaData> findNextMangaToRead(@Param("userId") Long userId);
    Optional<MangaData> findFirstByIdGreaterThanOrderByIdAsc(Long lastMangaId);

    @Query(value = """
        SELECT m FROM MangaData m
        WHERE m.id>:id
        ORDER BY m.id asc
        LIMIT 1
        """)
    Optional<MangaData> findNextAfterId(@Param("id") Long id);

    @Modifying
    @Query("UPDATE MangaData m SET m.countChapters = :chapters WHERE m.id = :id")
    void updateChaptersCount(@Param("id") Long id, @Param("chapters") int chapters);

    // Обновление времени последнего обновления
    @Modifying
    @Query("UPDATE MangaData m SET m.lastUpdated = CURRENT_TIMESTAMP WHERE m.id = :id")
    void refreshLastUpdated(@Param("id") Long id);

}