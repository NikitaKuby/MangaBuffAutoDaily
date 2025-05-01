package ru.finwax.mangabuffjob.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MangaController {
    private final MangaBuffAuth mangaBuffAuth;
    private final MbAuth mangaAuth;
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


    @GetMapping("/kill")
    public void kill(){
        killChromeDrivers();
    }

    @GetMapping("/actual/{id}")
    public void getActual(@PathVariable Long id){
        mangaAuth.getActualDriver(id, "actual").quit();
    }



    @GetMapping("/update")
    public void createMangaDb(){
        mangaBuffAuth.authenticate();
    }

    @GetMapping("/comment/{id}")
    public void getChId(@PathVariable Long id){
        commentScheduler.startDailyCommentSending(mangaAuth.getActualDriver(id, "COmment"), id);
    }

    @GetMapping("/start")
    public void start(){
        schedulerService.startScheduledPlan();
    }

    @GetMapping("/mine/{id}")
    public void mine(@PathVariable Long id){
        mineScheduler.performMining(mangaAuth.getActualDriver(id, "mine"));
    }

    @GetMapping("/quiz/{id}")
    public void quiz(@PathVariable Long id){
        quizScheduler.monitorQuizRequests(mangaAuth.getActualDriver(id, "quiz"));
    }

    @GetMapping("/read/{id}")
    public void startReading(@PathVariable Long id){
        mangaReadScheduler.readMangaChapters(mangaAuth.getActualDriver(id, "reader"),id, 2);
    }
//
    @GetMapping("/gifts")
    public String getGift(){
        return mangaReadScheduler.getAllGiftCounts().toString();
    }

    @GetMapping("/adv/{id}")
    public void advClick(@PathVariable Long id){
        advertisingScheduler.performAdv(mangaAuth.getActualDriver(id, "adv"));
    }

    public static void killChromeDrivers() {
        try {
            Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe /T");
            Runtime.getRuntime().exec("taskkill /F /IM chrome.exe /T");
            log.info("УСПЕШНОЕ УБИЙСТВО ДРАЙВЕРОВ");
        } catch (IOException e) {
            log.warn("Failed to kill chrome processes", e);
        }
    }




}
