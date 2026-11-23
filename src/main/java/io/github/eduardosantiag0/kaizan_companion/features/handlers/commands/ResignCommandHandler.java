package io.github.eduardosantiag0.kaizan_companion.features.handlers.commands;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.JobStatus;
import io.github.eduardosantiag0.kaizan_companion.exceptions.telegram.CommandMisuseException;
import io.github.eduardosantiag0.kaizan_companion.domain.models.ECommands;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.RunPodCancelClient;
import io.github.eduardosantiag0.kaizan_companion.features.handlers.ICommandHandler;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.NotificationService;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.replies.ReplyWithText;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

@Service
public class ResignCommandHandler implements ICommandHandler {

    private final NotificationService notificationService;
    private final RunPodCancelClient runPodCancelClient;

    public ResignCommandHandler(NotificationService notificationService, RunPodCancelClient runPodCancelClient) {
        this.notificationService = notificationService;
        this.runPodCancelClient = runPodCancelClient;
    }

    @Override
    public ECommands getCommand() {
        return ECommands.RESIGN;
    }

    @Override
    public void executeCommand(Update update) throws TelegramApiException, IOException {
        String[] fullMessage = update.getMessage().getText().split("\\s+");
        Long chatId = update.getMessage().getChatId();

        if (fullMessage.length < 2) {
            notificationService.sendErrorMessage(new ReplyWithText(chatId, new CommandMisuseException(getCommand()).getMessage()));
            return;
        }

        String jobId = fullMessage[1];

        JobStatus status = runPodCancelClient.cancel(jobId);

        String text;

        if (status == JobStatus.CANCELLED) {
            text = "Job: " + jobId + " was cancelled\n";
        } else {
            text = "It was not possible to cancel job: " + jobId;
        }

        notificationService.sendMessageToChat(
                new ReplyWithText(chatId,
                        text
                )
        );
    }
}
