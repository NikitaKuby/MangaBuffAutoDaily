package ru.finwax.mangabuffjob.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MangaTask {
    private Long userId;
    private TaskType type;
    private int remainingCount;
    private TaskStatus status;
    private String errorMessage;

    public MangaTask(Long userId, TaskType type, int remainingCount) {
        this.userId = userId;
        this.type = type;
        this.remainingCount = remainingCount;
        this.status = TaskStatus.PENDING;
    }
} 