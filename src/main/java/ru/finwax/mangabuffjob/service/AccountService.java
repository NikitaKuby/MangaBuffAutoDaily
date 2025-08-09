package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.finwax.mangabuffjob.Entity.MangaProgress;
import ru.finwax.mangabuffjob.Entity.UserCookie;
import ru.finwax.mangabuffjob.repository.MangaChapterRepository;
import ru.finwax.mangabuffjob.repository.MangaProgressRepository;
import ru.finwax.mangabuffjob.repository.MangaReadingProgressRepository;
import ru.finwax.mangabuffjob.repository.UserCookieRepository;
import ru.finwax.mangabuffjob.repository.GiftStatRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserCookieRepository userCookieRepository;
    private final MangaProgressRepository mangaProgressRepository;
    private final MangaChapterRepository mangaChapterRepository;
    private final MangaReadingProgressRepository mangaReadingProgressRepository;
    private final GiftStatRepository giftStatRepository;

    @Transactional
    public void deleteAccount(String username) {
        UserCookie userCookie = userCookieRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Account not found: " + username));
        
        Long userId = userCookie.getId();
        
        // Удаляем связанные записи
        mangaChapterRepository.deleteByUserId(userId);
        mangaReadingProgressRepository.deleteByUserCookieId(userId);
        mangaProgressRepository.deleteByUserId(userId);
        giftStatRepository.deleteByUserId(userId);
        
        // Удаляем сам аккаунт
        userCookieRepository.delete(userCookie);
    }

    @Transactional
    public void updateAccountProgressEnabledStates(Long userId, boolean readerEnabled, boolean commentEnabled, boolean quizEnabled, boolean mineEnabled, boolean advEnabled) {
        mangaProgressRepository.findByUserId(userId)
            .ifPresent(progress -> {
                progress.setReaderEnabled(readerEnabled);
                progress.setCommentEnabled(commentEnabled);
                progress.setQuizEnabled(quizEnabled);
                progress.setMineEnabled(mineEnabled);
                progress.setAdvEnabled(advEnabled);
                mangaProgressRepository.save(progress);
            });
    }

    @Transactional
    public void updateAccountOrder(List<Long> userIdOrder) {
        for (int i = 0; i < userIdOrder.size(); i++) {
            Long userId = userIdOrder.get(i);
            mangaProgressRepository.updateDisplayOrder(userId, i);
        }
    }

    /**
     * Получает все аккаунты в правильном порядке отображения
     */
    public List<UserCookie> getAllAccountsInOrder() {
        // Получаем все прогрессы с сортировкой по displayOrder
        List<MangaProgress> sortedProgresses = mangaProgressRepository.findAllOrderByDisplayOrder();
        
        // Получаем все аккаунты
        List<UserCookie> allAccounts = userCookieRepository.findAll();
        
        // Создаем Map для быстрого поиска аккаунта по userId
        Map<Long, UserCookie> accountMap = allAccounts.stream()
            .collect(Collectors.toMap(UserCookie::getId, a -> a));
        
        // Возвращаем аккаунты в порядке их displayOrder
        return sortedProgresses.stream()
            .map(progress -> accountMap.get(progress.getUserId()))
            .filter(account -> account != null)
            .collect(Collectors.toList());
    }
} 