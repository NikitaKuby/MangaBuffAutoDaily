package ru.finwax.mangabuffjob.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

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
    private Long diamond;
    private boolean reloginRequired;
    private boolean readerEnabled;
    private boolean commentEnabled;
    private boolean quizEnabled;
    private boolean mineEnabled;
    private boolean advEnabled;
    private Map<String, CountScroll> scrollCounts;
    private Integer mineCoin;
    private Integer mineLvl;
    private boolean autoUpgradeEnabled;
    private boolean autoExchangeEnabled;

    public AccountProgress(String username, String readerProgress, String commentProgress, Boolean quizDone,
                         String mineProgress, String advProgress, Integer advDone, String avatarPath,
                         String avatarAltText, Long userId, Integer totalReaderChapters,
                         Integer totalCommentChapters, Integer mineHitsLeft, Long diamond,
                         boolean reloginRequired, boolean readerEnabled, boolean commentEnabled,
                         boolean quizEnabled, boolean mineEnabled, boolean advEnabled,
                         Map<String, CountScroll> scrollCounts, Integer mineCoin, Integer mineLvl,
                         boolean autoUpgradeEnabled, boolean autoExchangeEnabled) {
        this.username = username;
        this.readerProgress = readerProgress;
        this.commentProgress = commentProgress;
        this.quizDone = quizDone;
        this.mineProgress = mineProgress;
        this.mineHitsLeft = mineHitsLeft;
        this.advProgress = advProgress;
        this.advDone = advDone;
        this.avatarPath = avatarPath;
        this.avatarAltText = avatarAltText;
        this.userId = userId;
        this.totalReaderChapters = totalReaderChapters;
        this.totalCommentChapters = totalCommentChapters;
        this.diamond = diamond;
        this.reloginRequired = reloginRequired;
        this.readerEnabled = readerEnabled;
        this.commentEnabled = commentEnabled;
        this.quizEnabled = quizEnabled;
        this.mineEnabled = mineEnabled;
        this.advEnabled = advEnabled;
        this.scrollCounts = scrollCounts;
        this.mineCoin = mineCoin;
        this.mineLvl = mineLvl;
        this.autoUpgradeEnabled = autoUpgradeEnabled;
        this.autoExchangeEnabled = autoExchangeEnabled;
    }

    public boolean isReloginRequired() {
        return reloginRequired;
    }

    public void setReloginRequired(boolean reloginRequired) {
        this.reloginRequired = reloginRequired;
    }

    public boolean isReaderEnabled() {
        return readerEnabled;
    }

    public void setReaderEnabled(boolean readerEnabled) {
        this.readerEnabled = readerEnabled;
    }

    public boolean isCommentEnabled() {
        return commentEnabled;
    }

    public void setCommentEnabled(boolean commentEnabled) {
        this.commentEnabled = commentEnabled;
    }

    public boolean isQuizEnabled() {
        return quizEnabled;
    }

    public void setQuizEnabled(boolean quizEnabled) {
        this.quizEnabled = quizEnabled;
    }

    public boolean isMineEnabled() {
        return mineEnabled;
    }

    public void setMineEnabled(boolean mineEnabled) {
        this.mineEnabled = mineEnabled;
    }

    public boolean isAdvEnabled() {
        return advEnabled;
    }

    public void setAdvEnabled(boolean advEnabled) {
        this.advEnabled = advEnabled;
    }

    public boolean isAutoUpgradeEnabled() {
        return autoUpgradeEnabled;
    }

    public void setAutoUpgradeEnabled(boolean autoUpgradeEnabled) {
        this.autoUpgradeEnabled = autoUpgradeEnabled;
    }

    public boolean isAutoExchangeEnabled() {
        return autoExchangeEnabled;
    }

    public void setAutoExchangeEnabled(boolean autoExchangeEnabled) {
        this.autoExchangeEnabled = autoExchangeEnabled;
    }

    public int getTotalScrolls() {
        if (scrollCounts == null) return 0;
        return scrollCounts.values().stream().mapToInt(CountScroll::getCount).sum();
    }
} 