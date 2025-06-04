package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.finwax.mangabuffjob.Entity.GiftStatistic;

@Repository
public interface GiftStatRepository extends JpaRepository<GiftStatistic, Long> {
    @Modifying
    @Query("DELETE FROM GiftStatistic g WHERE g.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
} 