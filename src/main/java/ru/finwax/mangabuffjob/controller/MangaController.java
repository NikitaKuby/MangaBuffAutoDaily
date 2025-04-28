package ru.finwax.mangabuffjob.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.finwax.mangabuffjob.Sheduled.service.AdvertisingScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.CommentScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MangaReadScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MineScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.QuizScheduler;
import ru.finwax.mangabuffjob.Sheduled.SchedulerService;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.service.ChapterThanksGeneratorService;
import ru.finwax.mangabuffjob.service.CommentService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MangaController {
    private final MangaBuffAuth mangaBuffAuth;
    private final MbAuth mangaAuth;
    private final CommentService commentService;
    private final ChapterThanksGeneratorService chapterThanksGeneratorService;
    private final CommentScheduler commentScheduler;
    private final MineScheduler mineScheduler;
    private final QuizScheduler quizScheduler;
    private final MangaReadScheduler mangaReadScheduler;
    private final SchedulerService schedulerService;
    private final AdvertisingScheduler advertisingScheduler;

    @GetMapping("/auth")
    public void authMangaBuff(){
        schedulerService.startScheduledPlan();
    }

    @GetMapping("/actual/{id}")
    public void getActual(@PathVariable Long id){
        mangaAuth.getActualDriver(id).quit();
    }

    @GetMapping("/update")
    public void createMangaDb(){
        mangaBuffAuth.authenticate();
    }


    @GetMapping("/comment")
    public void sendPostRequestWithCookies() {
        // Отправляем запрос
        commentService.sendPostRequestWithCookies(chapterThanksGeneratorService.generateThanks(), "121942", 1L);
    }


    @GetMapping("/id")
    public void getChId(){
        commentScheduler.startDailyCommentSending(1L);
    }

    @GetMapping("/start")
    public void start(){
        schedulerService.startScheduledPlan();
    }

    @GetMapping("/mine")
    public void mine(){

        mineScheduler.performMining(mangaAuth.getActualDriver(1L));
        mineScheduler.performMining(mangaAuth.getActualDriver(3L));
    }

    @GetMapping("/quiz")
    public void quiz(){
        quizScheduler.monitorQuizRequests(mangaAuth.getActualDriver(1L));
    }

    @GetMapping("/quiz2")
    public void quiz2(){
        quizScheduler.monitorQuizRequests(mangaAuth.getActualDriver(1L));
    }

    @GetMapping("/read/{id}")
    public void startReading(@PathVariable Long id){
        mangaReadScheduler.readMangaChapters(mangaAuth.getActualDriver(id),id);
    }

    @GetMapping("/gifts")
    public String getGift(){
        return mangaReadScheduler.getAllGiftCounts().toString();
    }

    @GetMapping("/adv")
    public void advClick(){
        advertisingScheduler.performAdv(mangaAuth.getActualDriver(1L));
    }




}
