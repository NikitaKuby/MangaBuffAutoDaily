package ru.finwax.mangabuffjob.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.finwax.mangabuffjob.model.MangaTask;
import ru.finwax.mangabuffjob.model.TaskStatus;
import ru.finwax.mangabuffjob.model.TaskType;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskExecutor {
    private ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final BlockingQueue<MangaTask> taskQueue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, MangaTask> taskStatuses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicBoolean> activeAccountTasks = new ConcurrentHashMap<>();
    @Getter
    private final TaskExecutionService taskExecutionService;
    private volatile boolean isRunning = false;

    public List<MangaTask> getRunningTasks() {
        List<MangaTask> runningTasks = new ArrayList<>();
        for (MangaTask task : taskStatuses.values()) {
            if (task.getStatus() == TaskStatus.RUNNING) {
                runningTasks.add(task);
            }
        }
        return runningTasks;
    }

    private String getTaskKey(Long userId, TaskType type) {
        return userId + ":" + type.name();
    }

    public void executeTasks(List<MangaTask> tasks, Consumer<MangaTask> statusCallback) {
        if (isRunning) {
            return;
        }
        isRunning = true;

        // Если executorService был остановлен, создаем новый
        if (executorService == null || executorService.isTerminated()) {
            executorService = Executors.newFixedThreadPool(3);
        }

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
                // Получаем задачу из очереди
                MangaTask task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    if (taskQueue.isEmpty()) {
                        isRunning = false;
                        break;
                    }
                    continue;
                }

                // Проверяем, нет ли уже активной задачи для этого аккаунта
                AtomicBoolean accountActive = activeAccountTasks.computeIfAbsent(task.getUserId(), k -> new AtomicBoolean(false));
                if (!accountActive.compareAndSet(false, true)) {
                    // Если для аккаунта уже есть активная задача, возвращаем текущую в очередь
                    taskQueue.offer(task);
                    continue;
                }

                try {
                    // Обновляем статус задачи
                    task.setStatus(TaskStatus.RUNNING);
                    taskStatuses.put(getTaskKey(task.getUserId(), task.getType()), task);
                    statusCallback.accept(task);

                    // Выполняем задачу
                    taskExecutionService.executeTask(task);
                    task.setStatus(TaskStatus.COMPLETED);
                    taskStatuses.put(getTaskKey(task.getUserId(), task.getType()), task);
                } catch (Exception e) {
                    task.setStatus(TaskStatus.ERROR);
                    task.setErrorMessage(e.getMessage());
                    taskStatuses.put(getTaskKey(task.getUserId(), task.getType()), task);
                } finally {
                    // Освобождаем флаг активности для аккаунта
                    accountActive.set(false);
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

    public void stopAllTasks() {
        // Останавливаем выполнение новых задач
        isRunning = false;
        
        // Очищаем очередь задач
        taskQueue.clear();
        
        // Обновляем статус всех выполняющихся задач на ERROR
        for (MangaTask task : taskStatuses.values()) {
            if (task.getStatus() == TaskStatus.RUNNING) {
                task.setStatus(TaskStatus.ERROR);
                task.setErrorMessage("Task stopped by user");
            }
        }
        
        // Очищаем активные задачи для аккаунтов
        activeAccountTasks.clear();
        
        // Останавливаем executor service
        executorService.shutdownNow();
        executorService = null;
    }
} 