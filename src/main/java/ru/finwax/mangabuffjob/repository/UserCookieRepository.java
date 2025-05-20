package ru.finwax.mangabuffjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import ru.finwax.mangabuffjob.Entity.UserCookie;

import java.util.Optional;

@Repository
public interface UserCookieRepository extends JpaRepository<UserCookie, Long> {
    Optional<UserCookie> findByUsername(String username);
    
    @Modifying
    void deleteByUsername(String username);
}