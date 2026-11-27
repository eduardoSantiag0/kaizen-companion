package io.github.eduardosantiag0.kaizan_companion.features.handlers;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.TelegramChatRepository;
import io.github.eduardosantiag0.kaizan_companion.exceptions.sgf.EmptySgfException;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.messaging.AnalysisProducer;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.NotificationService;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.replies.ReplyWithText;
import io.github.eduardosantiag0.kaizan_companion.utils.MessageFormatter;
import io.github.eduardosantiag0.kaizan_companion.utils.SgfValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@Service
public class InputFileService {
    private final NotificationService notificationService;
    private final TelegramChatRepository telegramChatRepository;
    private final AnalysisProducer analysisProducer;
    private final RestTemplate rest = new RestTemplate();
    private final String BOT_TOKEN;



    public InputFileService(NotificationService notificationService,
                            TelegramChatRepository telegramChatRepository,
                            AnalysisProducer analysisProducer,
                            @Value("${telegram.bot.token}")
                            String botToken
    ) {
        this.notificationService = notificationService;
        this.telegramChatRepository = telegramChatRepository;
        this.analysisProducer = analysisProducer;
        BOT_TOKEN = botToken;
    }

    public void executeCommand(Update update) throws TelegramApiException {

        Long chatId = update.getMessage().getChatId();
        Document document = update.getMessage().getDocument();

        try {
            SgfValidator.validateMetadata(
                    document.getFileName(),
                    document.getFileSize()
            );

            String getFileUrl = "https://api.telegram.org/bot"
                    + BOT_TOKEN + "/getFile?file_id={fileId}";

            Map<?, ?> response = rest.getForObject(
                    getFileUrl,
                    Map.class,
                    document.getFileId()
            );

            if (response == null ||
                    !Boolean.TRUE.equals(response.get("ok"))) {
                throw new IOException("Failed to retrieve file metadata");
            }

            Map<?, ?> result = (Map<?, ?>) response.get("result");

            if (result == null ||
                    !(result.get("file_path") instanceof String filePath)) {
                throw new IOException("File path not found");
            }

            String downloadUrl = "https://api.telegram.org/file/bot"
                    + BOT_TOKEN + "/" + filePath;

            byte[] fileData = rest.getForObject(
                    downloadUrl,
                    byte[].class
            );

            if (!SgfValidator.validate(fileData)) {
                throw new IllegalArgumentException("Invalid SGF file.");
            }

            var chat = update.getMessage().getChat();

            TelegramChatEntity entity = telegramChatRepository
                    .findById(chatId)
                    .orElseGet(() -> new TelegramChatEntity(
                            chatId,
                            chat.getFirstName(),
                            chat.getUserName(),
                            chat.getType(),
                            true
                    ));

            entity.setWaitingForAnalysis(true);
            telegramChatRepository.save(entity);

            analysisProducer.execute(
                    chatId,
                    new ByteArrayResource(fileData)
            );

            notificationService.sendMessageToChat(
                    new ReplyWithText(
                            chatId,
                            MessageFormatter.requestReceived()
                    )
            );

        } catch (IllegalArgumentException e) {

            log.warn("Invalid file. Chat: {}", chatId);

            notificationService.sendErrorMessage(
                    new ReplyWithText(chatId, e.getMessage())
            );

        } catch (IOException | RestClientException e) {

            log.error(
                    "Failed to process file. Chat: {}. Error: {}",
                    chatId,
                    e.getClass().getSimpleName()
            );

            notificationService.sendErrorMessage(
                    new ReplyWithText(
                            chatId,
                            MessageFormatter.requestFailed()
                    )
            );
        }
    }

}
