package ru.finwax.mangabuffjob.model;

public enum TaskType {
    ADV(1),
    MINE(2),
    QUIZ(3),
    COMMENT(4),
    READER(5);

    private final int priority;

    TaskType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
} 