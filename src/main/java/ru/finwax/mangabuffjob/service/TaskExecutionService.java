package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.model.MangaTask;
import ru.finwax.mangabuffjob.model.TaskType;
import ru.finwax.mangabuffjob.Sheduled.service.AdvertisingScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MineScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.QuizScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.CommentScheduler;
import ru.finwax.mangabuffjob.Sheduled.service.MangaReadScheduler;

@Service
@RequiredArgsConstructor
public class TaskExecutionService {
    private final ScanningProgress scanningProgress;
    private final AdvertisingScheduler advertisingScheduler;
    private final MineScheduler mineScheduler;
    private final QuizScheduler quizScheduler;
    private final CommentScheduler commentScheduler;
    private final MangaReadScheduler mangaReadScheduler;
    
    private boolean checkViews = false;

    public void setCheckViews(boolean checkViews) {
        this.checkViews = checkViews;
    }

    public void executeTask(MangaTask task) {
        switch (task.getType()) {
            case ADV:
                executeAdvTask(task);
                break;
            case MINE:
                executeMineTask(task);
                break;
            case QUIZ:
                executeQuizTask(task);
                break;
            case COMMENT:
                executeCommentTask(task);
                break;
            case READER:
                executeReaderTask(task);
                break;
        }
    }

    private void executeAdvTask(MangaTask task) {
        try {
            advertisingScheduler.performAdv(task.getUserId(), task.getRemainingCount(), checkViews);
            Thread.sleep(10000);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при выполнении рекламы: " + e.getMessage(), e);
        }
    }

    private void executeMineTask(MangaTask task) {
        try {
            mineScheduler.performMining(task.getUserId(), task.getRemainingCount(), checkViews);
            Thread.sleep(10000);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при выполнении майнинга: " + e.getMessage(), e);
        }
    }

    private void executeQuizTask(MangaTask task) {
        try {
            quizScheduler.monitorQuizRequests(task.getUserId(), checkViews);
            Thread.sleep(10000);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при выполнении квиза: " + e.getMessage(), e);
        }
    }

    private void executeCommentTask(MangaTask task) {
        try {
            commentScheduler.startDailyCommentSending(task.getUserId(), task.getRemainingCount()).get();
            Thread.sleep(10000);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при выполнении комментариев: " + e.getMessage(), e);
        }
    }

    private void executeReaderTask(MangaTask task) {
        try {
            mangaReadScheduler.readMangaChapters(task.getUserId(), task.getRemainingCount(), checkViews);
            Thread.sleep(10000);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при выполнении чтения: " + e.getMessage(), e);
        }
    }
} 