package io.github.eduardosantiag0.kaizan_companion.features.handlers.commands;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.AnalysisJobEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.AnalysisJobRepository;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.JobCancellationService;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.NotificationService;
import io.github.eduardosantiag0.kaizan_companion.features.notifications.replies.ReplyWithText;
import io.github.eduardosantiag0.kaizan_companion.support.TelegramFixtures;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class ResignCommandHandlerTest {

    @Test
    void executeCommand_onMissingArgument_mustReplyWithUsage_insteadOfThrowing() {
        NotificationService notificationService = mock(NotificationService.class);
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
        ResignCommandHandler handler = new ResignCommandHandler(
                notificationService, mock(JobCancellationService.class), jobRepository);

        var update = TelegramFixtures.textUpdate(1L, "/resign");

        assertDoesNotThrow(() -> handler.executeCommand(update));
        verify(notificationService).sendErrorMessage(any(ReplyWithText.class));
    }

    @Test
    void executeCommand_onUnknownCorrelationId_repliesNotCancelled_insteadOfThrowing() {
        NotificationService notificationService = mock(NotificationService.class);
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
        JobCancellationService jobCancellationService = mock(JobCancellationService.class);
        ResignCommandHandler handler = new ResignCommandHandler(notificationService, jobCancellationService, jobRepository);

        UUID correlationId = UUID.randomUUID();
        when(jobRepository.findByCorrelationId(correlationId)).thenReturn(Optional.empty());

        var update = TelegramFixtures.textUpdate(1L, "/resign " + correlationId);

        assertDoesNotThrow(() -> handler.executeCommand(update));

        verify(notificationService).sendMessageToChat(argThat((ReplyWithText reply) ->
                reply.text().contains("not possible to cancel")));
    }

    @Test
    void executeCommand_onKnownCorrelationId_cancelsTheResolvedJob_notTheTypedText() {
        NotificationService notificationService = mock(NotificationService.class);
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
        JobCancellationService jobCancellationService = mock(JobCancellationService.class);
        ResignCommandHandler handler = new ResignCommandHandler(notificationService, jobCancellationService, jobRepository);

        TelegramChatEntity chat = new TelegramChatEntity(1L, "Ana", "ana", "private", false);
        UUID correlationId = UUID.randomUUID();
        AnalysisJobEntity job = new AnalysisJobEntity(chat, Instant.now(), correlationId);

        when(jobRepository.findByCorrelationId(correlationId)).thenReturn(Optional.of(job));
        when(jobCancellationService.cancel(job)).thenReturn(true);

        var update = TelegramFixtures.textUpdate(1L, "/resign " + correlationId);

        assertDoesNotThrow(() -> handler.executeCommand(update));

        verify(jobCancellationService).cancel(job);
        verify(notificationService).sendMessageToChat(argThat((ReplyWithText reply) ->
                reply.text().contains("was cancelled")));
    }
}
