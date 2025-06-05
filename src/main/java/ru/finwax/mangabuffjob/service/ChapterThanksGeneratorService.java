package ru.finwax.mangabuffjob.service;

import org.springframework.stereotype.Service;

import java.util.Random;
@Service
public class ChapterThanksGeneratorService {

    private static final String[] THANKS_TEMPLATES = {

    };

    private static final String[] ADDITIONAL_PHRASES = {

    };

    private static final String[] PUNCTUATION_MARKS = {  };

    private final Random random = new Random();

    public String generateThanks() {
        String template = THANKS_TEMPLATES[random.nextInt(THANKS_TEMPLATES.length)];
        String additional = ADDITIONAL_PHRASES[random.nextInt(ADDITIONAL_PHRASES.length)];
        String punctuation = PUNCTUATION_MARKS[random.nextInt(PUNCTUATION_MARKS.length)];

        String result = String.format(template, punctuation, additional);

        while (result.length() < 10) {
            result += " " + ADDITIONAL_PHRASES[random.nextInt(ADDITIONAL_PHRASES.length)];
        }


        return result;
    }
}