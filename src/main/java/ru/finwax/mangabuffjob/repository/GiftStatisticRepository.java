package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finwax.mangabuffjob.Entity.GiftStatistic;

import java.time.LocalDateTime;
import java.util.Optional;

public interface GiftStatisticRepository extends JpaRepository<GiftStatistic, Long> {
    Optional<GiftStatistic> findByUserIdAndDate(Long userId, LocalDateTime date);
}
