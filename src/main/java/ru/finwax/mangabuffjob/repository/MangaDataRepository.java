package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.finwax.mangabuffjob.Entity.MangaData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MangaDataRepository extends JpaRepository<MangaData, Long> {

    boolean existsByUrl(String url);

    Optional<MangaData> findFirstByOrderByIdAsc();

    @Query(value = """
    SELECT m FROM MangaData m
    LEFT JOIN MangaReadingProgress p ON m.id = p.manga.id
    WHERE (p.hasReaded = false AND p.chapterReaded > 0) OR p.manga.id IS NULL
    ORDER BY 
        CASE WHEN p.manga.id IS NULL THEN 1 ELSE 0 END,
        p.chapterReaded ASC,
        m.id ASC
    LIMIT 1
    """)
    Optional<MangaData> findNextMangaToRead();
    Optional<MangaData> findFirstByIdGreaterThanOrderByIdAsc(Long id);

    @Modifying
    @Query("UPDATE MangaData m SET m.countChapters = :chapters WHERE m.id = :id")
    void updateChaptersCount(@Param("id") Long id, @Param("chapters") int chapters);

    // Обновление времени последнего обновления
    @Modifying
    @Query("UPDATE MangaData m SET m.lastUpdated = CURRENT_TIMESTAMP WHERE m.id = :id")
    void refreshLastUpdated(@Param("id") Long id);

}