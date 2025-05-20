package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.finwax.mangabuffjob.Entity.MangaProgress;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MangaProgressRepository extends JpaRepository<MangaProgress, Long> {
    Optional<MangaProgress> findByUserId(Long userId);
    boolean existsByUserIdIs(Long userId);
    
    @Modifying
    void deleteByUserId(Long userId);

    @Modifying
    @Query(value = """
        UPDATE mangabuff_progress m SET
        m.reader_done = :readerDone,
        m.comment_done = :commentDone,
        m.total_reader_chapters = :totalReaderChapters,
        m.total_comment_chapters = :totalCommentChapters,
        m.quiz_done = :quizDone,
        m.mine_done = :mineDone,
        m.adv_done = :advDone,
        m.lastUpdated = :lastUpdated
        WHERE m.user_id = :userId
        """, nativeQuery=true)
    void updateProgress(
        @Param("userId") Long userId,
        @Param("readerDone") Integer readerDone,
        @Param("commentDone") Integer commentDone,
        @Param("totalReaderChapters") Integer totalReaderChapters,
        @Param("totalCommentChapters") Integer totalCommentChapters,
        @Param("quizDone") Boolean quizDone,
        @Param("mineDone") Boolean mineDone,
        @Param("advDone") Boolean advDone,
        @Param("lastUpdated") LocalDate lastUpdated);

    @Modifying
    @Query(value = """
        UPDATE mangabuff_progress m SET
        m.reader_done = :readerDone,
        m.comment_done = :commentDone,
        m.total_reader_chapters = :totalReaderChapters,
        m.total_comment_chapters = :totalCommentChapters,
        m.lastUpdated = :lastUpdated
        WHERE m.user_id = :userId
        """, nativeQuery=true)
    void updateProgressComAndRead(
        @Param("userId") Long userId,
        @Param("readerDone") Integer readerDone,
        @Param("commentDone") Integer commentDone,
        @Param("totalReaderChapters") Integer totalReaderChapters,
        @Param("totalCommentChapters") Integer totalCommentChapters,
        @Param("lastUpdated") LocalDate lastUpdated);

}