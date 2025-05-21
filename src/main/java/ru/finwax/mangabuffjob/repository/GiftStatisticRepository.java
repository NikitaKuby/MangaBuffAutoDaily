package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.finwax.mangabuffjob.Entity.GiftStatistic;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GiftStatisticRepository extends JpaRepository<GiftStatistic, Long> {
    Optional<GiftStatistic> findByUserIdAndDate(Long userId, LocalDate date);
}
