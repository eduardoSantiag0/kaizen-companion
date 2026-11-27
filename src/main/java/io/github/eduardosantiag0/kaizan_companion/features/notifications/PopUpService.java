package io.github.eduardosantiag0.kaizan_companion.features.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Service
public class PopUpService {

    private final RestClient restClient;
    private static final String MESSAGE = """
        🎉 Your game analysis is ready!

        Every move is an opportunity to improve. 🌱
        """;

    public PopUpService(@Value("${telegram.bot.token}") String botToken) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + botToken)
                .build();
    }


    public void execute(Long chatId) {
        var payload = new TelegramMessageRequest(chatId, MESSAGE);

        restClient.post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private record TelegramMessageRequest(Long chat_id, String text) {}
}
