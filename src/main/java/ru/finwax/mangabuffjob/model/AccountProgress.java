package ru.finwax.mangabuffjob.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AccountProgress {
    private String username;
    private String readerProgress;
    private String commentProgress;
    private Boolean quizDone;
    private String mineProgress;
    private Integer mineHitsLeft;
    private String advProgress;
    private Integer advDone;
    private String avatarPath;
    private String avatarAltText;
    private Long userId;
    private Integer totalReaderChapters;
    private Integer totalCommentChapters;

    public AccountProgress(String username, String readerProgress, String commentProgress, Boolean quizDone, String mineProgress, String advProgress, Integer advDone, String avatarPath, String avatarAltText, Long userId, Integer totalReaderChapters, Integer totalCommentChapters) {
        this.username = username;
        this.readerProgress = readerProgress;
        this.commentProgress = commentProgress;
        this.quizDone = quizDone;
        this.mineProgress = mineProgress;
        this.advProgress = advProgress;
        this.advDone = advDone;
        this.avatarPath = avatarPath;
        this.avatarAltText = avatarAltText;
        this.userId = userId;
        this.totalReaderChapters = totalReaderChapters;
        this.totalCommentChapters = totalCommentChapters;
    }

    public AccountProgress(String username, String readerProgress, String commentProgress, Boolean quizDone, String mineProgress, String advProgress, Integer advDone, String avatarPath, String avatarAltText, Long userId, Integer totalReaderChapters, Integer totalCommentChapters, Integer mineHitsLeft) {
        this.username = username;
        this.readerProgress = readerProgress;
        this.commentProgress = commentProgress;
        this.quizDone = quizDone;
        this.mineProgress = mineProgress;
        this.advProgress = advProgress;
        this.advDone = advDone;
        this.avatarPath = avatarPath;
        this.avatarAltText = avatarAltText;
        this.userId = userId;
        this.totalReaderChapters = totalReaderChapters;
        this.totalCommentChapters = totalCommentChapters;
        this.mineHitsLeft = mineHitsLeft;
    }
} 