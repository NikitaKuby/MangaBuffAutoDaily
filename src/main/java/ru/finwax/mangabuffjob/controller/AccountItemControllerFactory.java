package ru.finwax.mangabuffjob.controller;

import javafx.fxml.FXMLLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.finwax.mangabuffjob.Sheduled.service.AdvertisingScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.CommentScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MangaReadScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MineScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.QuizScheduler;
import ru.finwax.mangabuffjob.service.AccountService;
import ru.finwax.mangabuffjob.repository.GiftStatisticRepository;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccountItemControllerFactory {
    private final AccountService accountService;
    private final AdvertisingScheduler advertisingScheduler;
    private final MineScheduler mineScheduler;
    private final QuizScheduler quizScheduler;
    private final CommentScheduler commentScheduler;
    private final MangaReadScheduler mangaReadScheduler;
    private final GiftStatisticRepository giftRepository;
    private final MbAuth mbAuth;
    private final MangaBuffAuth mangaBuffAuth;

    public AccountItemController createController(MangaBuffJobViewController parentController) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/finwax/mangabuffjob/view/AccountItem.fxml"));
        AccountItemController controller = new AccountItemController(
            accountService,
            parentController,
            advertisingScheduler,
            mineScheduler,
            quizScheduler,
            commentScheduler,
            mangaReadScheduler,
            giftRepository,
            mbAuth,
            mangaBuffAuth
        );
        loader.setController(controller);
        loader.load();
        mangaReadScheduler.setAccountItemController(controller);
        return controller;
    }
} 