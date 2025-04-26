package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.finwax.mangabuffjob.Entity.Cookie;

import java.util.Optional;

@Repository
public interface CookieRepository extends JpaRepository<Cookie, Long> {
    @Query("SELECT c FROM Cookie c ORDER BY c.id DESC LIMIT 1")
    Optional<Cookie> findTopByOrderByIdDesc();

}
