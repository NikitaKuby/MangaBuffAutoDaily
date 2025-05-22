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
        "Ого, уже новая глава? %s",
        "Как всегда круто! %s",
        "Ждал этого момента! %s",
        "Автор, вы гений! %s",
        "Не могу оторваться! %s",
        "Лучшее, что я сегодня читал %s",
        "Какой поворот! %s",
        "Это было неожиданно... %s",
        "Уже хочу продолжение! %s",
        "Перечитываю второй раз %s",
        "Мои эмоции: %s",
        "Затянуло с первых строк %s",
        "Почему так быстро закончилось? %s",
        "Идеально для вечера! %s",
        "Главный герой - это я? %s",
        "Слишком круто для слов %s",
        "Прям в точку! %s",
        "Этот момент... %s",
        "Хочу больше деталей! %s",
        "Как жить дальше? %s",
        "Мой день спасен! %s",
        "Стоило ждать! %s",
        "Лучшая глава в этом месяце %s",
        "Передайте автору спасибо! %s",
        "Спасибо автору за главу%s %s",
        "прочитал с удовольствием%s %s",
        "Спасиб за Главу%s %s"
    };

    private static final String[] ADDITIONAL_PHRASES = {
        "очень познавательно",
        "жду продолжения",
        "как же захватывающе",
        "не могу оторваться",
        "жду что будет дальше!",
        "как же это эпично!",
        "а вот это неожиданно",
        "перечитываю второй раз",
        "уже показываю друзьям",
        "этот персонаж - мой любимчик",
        "хочу такую же силу гг",
        "когда выйдет следующая?",
        "а вот здесь я прослезился",
        "смеялся до слез",
        "мурашки по коже",
        "не могу перестать думать об этом",
        "этот сюжетный поворот...",
        "лучше, чем в прошлой главе",
        "хочу больше экшен-сцен",
        "романтика на высоте",
        "комедийные моменты - огонь",
        "диалоги просто отпад",
        "визуал в голове - как в кино",
        "запоем читаю уже час",
        "переживаю за персонажей",
        "кто еще заметил эту деталь?",
        "эта сцена запала в душу",
        "автор точно гений сюжета",
        "хочу фанфик по этой вселенной",
        "когда будет аниме-адаптация?",
        "это достойно экранизации",
        "перечитываю любимые моменты",
        "вы просто мастер слова",
        "очень интересно"
    };

    private static final String[] PUNCTUATION_MARKS = { "!", ".", ",", "...","\uD83E\uDD23\uD83E\uDD23","❤️", "😍", "🤯" };

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