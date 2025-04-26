package ru.finwax.mangabuffjob.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.finwax.mangabuffjob.Sheduled.CommentScheduler;
import ru.finwax.mangabuffjob.Sheduled.MangaReadScheduler;
import ru.finwax.mangabuffjob.Sheduled.MineScheduler;
import ru.finwax.mangabuffjob.Sheduled.QuizScheduler;
import ru.finwax.mangabuffjob.auth.MangaBuffAuth;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
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
    private final CommentService commentService;
    private final ChapterThanksGeneratorService chapterThanksGeneratorService;
    private final CommentScheduler commentScheduler;
    private final MineScheduler mineScheduler;
    private final QuizScheduler quizScheduler;
    private final MangaReadScheduler mangaReadScheduler;

    @GetMapping("/update")
    public void createMangaDb(){
        mangaBuffAuth.authenticate();
    }

    @GetMapping("/update2")
    public void createMangaDb2(){
        mangaBuffAuth.refreshCookies();
    }

    @GetMapping("/comment")
    public void sendPostRequestWithCookies() {
        // Отправляем запрос
        commentService.sendPostRequestWithCookies(chapterThanksGeneratorService.generateThanks(), "121942");
    }

    @GetMapping("/id")
    public void getChId(){
        commentScheduler.startDailyCommentSending();
    }

    @GetMapping("/mine")
    public void mine(){
        mineScheduler.performMining();
    }

    @GetMapping("/quiz")
    public void quiz(){
        quizScheduler.monitorQuizRequests();
    }

    @GetMapping("/read")
    public void startReading(){
        mangaReadScheduler.readMangaChapters();
    }




}
