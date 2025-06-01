package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.finwax.mangabuffjob.Entity.GiftStatistic;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GiftStatisticRepository extends JpaRepository<GiftStatistic, Long> {
    List<GiftStatistic> findByUserIdAndDate(Long userId, LocalDate date);
}
