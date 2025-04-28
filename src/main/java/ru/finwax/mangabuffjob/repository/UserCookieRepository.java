package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finwax.mangabuffjob.Entity.UserCookie;

import java.util.Optional;

public interface UserCookieRepository extends JpaRepository<UserCookie, Long> {
    Optional<UserCookie> findByUsername(String username);
    void deleteByUsername(String username);
}