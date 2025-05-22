package ru.finwax.mangabuffjob.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.model.MangaTask;
import ru.finwax.mangabuffjob.model.TaskStatus;
import ru.finwax.mangabuffjob.model.TaskType;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class TaskExecutor {
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final BlockingQueue<MangaTask> taskQueue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<Long, TaskStatus> taskStatuses = new ConcurrentHashMap<>();
    private final TaskExecutionService taskExecutionService;
    private volatile boolean isRunning = false;

    public TaskExecutionService getTaskExecutionService() {
        return taskExecutionService;
    }

    public void executeTasks(List<MangaTask> tasks, Consumer<MangaTask> statusCallback) {
        if (isRunning) {
            return;
        }
        isRunning = true;

        // Сортируем задачи по приоритету
        tasks.sort(Comparator.comparingInt(t -> t.getType().getPriority()));
        
        // Добавляем задачи в очередь
        taskQueue.addAll(tasks);

        // Запускаем обработчики
        for (int i = 0; i < 3; i++) {
            executorService.submit(() -> processTasks(statusCallback));
        }
    }

    private void processTasks(Consumer<MangaTask> statusCallback) {
        while (isRunning) {
            try {
                MangaTask task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    if (taskQueue.isEmpty()) {
                        isRunning = false;
                        break;
                    }
                    continue;
                }

                // Обновляем статус задачи
                task.setStatus(TaskStatus.RUNNING);
                statusCallback.accept(task);

                try {
                    // Выполняем задачу
                    taskExecutionService.executeTask(task);
                    task.setStatus(TaskStatus.COMPLETED);
                } catch (Exception e) {
                    task.setStatus(TaskStatus.ERROR);
                    task.setErrorMessage(e.getMessage());
                }

                // Обновляем UI
                statusCallback.accept(task);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        isRunning = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
} 