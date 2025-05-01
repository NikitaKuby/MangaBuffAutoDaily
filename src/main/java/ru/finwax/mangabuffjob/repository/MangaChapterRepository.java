package ru.finwax.mangabuffjob.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.finwax.mangabuffjob.Entity.MangaChapter;

import java.util.List;
import java.util.Optional;

@Repository
public interface MangaChapterRepository extends JpaRepository<MangaChapter, Long> {

    @Modifying
    @Query("UPDATE MangaChapter c SET c.hasComment = true WHERE c.commentId IN :commentIds AND c.user.id = :userId")
    void markMultipleAsCommented(@Param("commentIds") List<String> commentIds,
                                 @Param("userId") Long userId);
    boolean existsByMangaIdAndChapterNumberAndUserId(Long mangaId, Integer chapterNumber, Long userId);
    @Query("SELECT COUNT(c) FROM MangaChapter c WHERE c.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    Optional<MangaChapter> findTopByUserIdOrderByIdDesc(Long userId);
    @Query("SELECT COUNT(c) FROM MangaChapter c WHERE c.hasComment = false AND c.user.id = :userId")
    long countUncommentedChapters(@Param("userId") Long userId);
    default boolean hasMoreThanTenUncommentedChapters(int count, Long id) {
        return countUncommentedChapters(id) >= count;
    }

    @Query("SELECT c.commentId FROM MangaChapter c WHERE c.user.id = :userId AND c.hasComment = false AND c.commentId IS NOT NULL ORDER BY c.id ASC")
    List<String> findFirstTenUncommentedChapterIds(Pageable pageable,
                                                   @Param("userId") Long userId);

}
