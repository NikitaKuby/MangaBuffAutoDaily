package ru.finwax.mangabuffjob.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.finwax.mangabuffjob.repository.MangaProgressRepository;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;

@Service
public class AccountService {
    private final UserCookieRepository userCookieRepository;
    private final MangaProgressRepository mangaProgressRepository;

    public AccountService(UserCookieRepository userCookieRepository, MangaProgressRepository mangaProgressRepository) {
        this.userCookieRepository = userCookieRepository;
        this.mangaProgressRepository = mangaProgressRepository;
    }

    @Transactional
    public void deleteAccount(String username) {
        userCookieRepository.findByUsername(username)
                .ifPresent(userCookie -> {
                    mangaProgressRepository.deleteByUserId(userCookie.getId());
                    userCookieRepository.deleteByUsername(username);
                });
    }
} 