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
        WHERE m.id>:id
        ORDER BY m.id asc
        LIMIT 1
        """)
    Optional<MangaData> findNextAfterId(@Param("id") Long id);

    @Query("SELECT m FROM MangaData m WHERE NOT EXISTS " +
        "(SELECT 1 FROM MangaChapter mc WHERE mc.manga.id = m.id AND mc.user.id = :userId AND mc.chapterNumber = -1) " +
        "ORDER BY m.id ASC")
    Optional<MangaData> findFirstByNotMarkedAsEmpty(@Param("userId") Long userId);

}