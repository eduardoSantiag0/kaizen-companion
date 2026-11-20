package io.github.eduardosantiag0.kaizan_companion.features.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

@Profile("dev")
@Component
public class TelegramLongPollingAdapter implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botName;
    private final String botToken;
    private final TelegramBotService telegramBotService;

    public TelegramLongPollingAdapter(
            @Value("${telegram.bot.name}") String botName,
            @Value("${telegram.bot.token}") String botToken,
            TelegramBotService telegramBotService) {
        this.botName = botName;
        this.botToken = botToken;
        this.telegramBotService = telegramBotService;
    }

    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        telegramBotService.consume(update);
    }
}
