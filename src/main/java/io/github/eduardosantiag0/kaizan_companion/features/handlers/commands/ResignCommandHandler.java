package io.github.eduardosantiag0.kaizan_companion.features.handlers.commands;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.AnalysisJobEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.AnalysisJobRepository;
import io.github.eduardosantiag0.kaizan_companion.exceptions.telegram.CommandMisuseException;
import io.github.eduardosantiag0.kaizan_companion.domain.models.ECommands;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.JobCancellationService;
import io.github.eduardosantiag0.kaizan_companion.features.handlers.ICommandHandler;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.NotificationService;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.replies.ReplyWithText;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class ResignCommandHandler implements ICommandHandler {

    private final NotificationService notificationService;
    private final JobCancellationService jobCancellationService;
    private final AnalysisJobRepository jobRepository;

    public ResignCommandHandler(NotificationService notificationService, JobCancellationService jobCancellationService,
                                 AnalysisJobRepository jobRepository) {
        this.notificationService = notificationService;
        this.jobCancellationService = jobCancellationService;
        this.jobRepository = jobRepository;
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

        String jobIdText = fullMessage[1];

        Optional<AnalysisJobEntity> job = parseJobId(jobIdText).flatMap(jobRepository::findByCorrelationId);

        String text;

        if (job.isPresent() && jobCancellationService.cancel(job.get())) {
            text = "Job: " + jobIdText + " was cancelled\n";
        } else {
            text = "It was not possible to cancel job: " + jobIdText;
        }

        notificationService.sendMessageToChat(
                new ReplyWithText(chatId,
                        text
                )
        );
    }

    private Optional<UUID> parseJobId(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
