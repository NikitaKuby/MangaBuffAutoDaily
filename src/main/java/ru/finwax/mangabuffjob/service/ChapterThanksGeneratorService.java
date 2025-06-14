package ru.finwax.mangabuffjob.service;

import org.springframework.stereotype.Service;

import java.util.Random;
@Service
public class ChapterThanksGeneratorService {

    private static final String[] THANKS_TEMPLATES = {
        "Благодарю за главу%s %s",
        "спасибо, интересная глава%s %s",
        "Отличная глава%s %s",
        "Спасибо за главу! %s",
        "Спасибо автору за главу%s %s",
        "Благодарочка>%s %s",
        "Спасибо автору за главу%s %s",
        "Спасиб за Главу%s %s"
    };

    private static final String[] ADDITIONAL_PHRASES = {
        ""
    };

    private static final String[] PUNCTUATION_MARKS = { "!", ".","❤️", "<3" };

    private final Random random = new Random();

    public String generateThanks() {
        String template = THANKS_TEMPLATES[random.nextInt(THANKS_TEMPLATES.length)];
        String additional = ADDITIONAL_PHRASES[random.nextInt(ADDITIONAL_PHRASES.length)];
        String punctuation = PUNCTUATION_MARKS[random.nextInt(PUNCTUATION_MARKS.length)];

        return String.format(template, punctuation, additional);
    }
}