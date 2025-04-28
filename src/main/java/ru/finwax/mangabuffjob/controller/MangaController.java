package ru.finwax.mangabuffjob.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.finwax.mangabuffjob.Sheduled.CommentScheduler;
import ru.finwax.mangabuffjob.Sheduled.MangaReadScheduler;
import ru.finwax.mangabuffjob.Sheduled.MineScheduler;
import ru.finwax.mangabuffjob.Sheduled.QuizScheduler;
import ru.finwax.mangabuffjob.Sheduled.SchedulerService;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.finwax.mangabuffjob.auth.MbAuth;
import ru.finwax.mangabuffjob.service.ChapterThanksGeneratorService;
import ru.finwax.mangabuffjob.service.CommentParserService;
import ru.finwax.mangabuffjob.service.CommentService;

import java.util.Collections;

import java.util.Set;

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

    @GetMapping("/auth")
    public void authMangaBuff(){
        schedulerService.startScheduledPlan();
    }

    @GetMapping("/actual")
    public void getActual(){
        mangaAuth.getActualDriver(2L).quit();
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

    @GetMapping("/mine")
    public void mine(){

        mineScheduler.performMining(mangaAuth.getActualDriver(1L));
        mineScheduler.performMining(mangaAuth.getActualDriver(2L));
    }

    @GetMapping("/quiz")
    public void quiz(){
        quizScheduler.monitorQuizRequests(mangaAuth.getActualDriver(1L));
    }

    @GetMapping("/quiz2")
    public void quiz2(){
        quizScheduler.monitorQuizRequests(mangaAuth.getActualDriver(1L));
    }

    @GetMapping("/read")
    public void startReading(){
        mangaReadScheduler.readMangaChapters();
    }




}
