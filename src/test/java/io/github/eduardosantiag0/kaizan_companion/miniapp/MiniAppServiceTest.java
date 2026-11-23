package io.github.eduardosantiag0.kaizan_companion.miniapp;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.AnalysisJobEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.JobStatus;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.GameAnalysisEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.AnalysisJobRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.GameAnalysisRepository;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.TelegramChatRepository;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.JobCancellationService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniAppServiceTest {

    private static void setCreatedAt(AnalysisJobEntity job, Instant createdAt) {
        try {
            Field field = AnalysisJobEntity.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(job, createdAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findAllByTelegramUserId_mergesCompletedAndInProgressJobs_newestFirst() {
        GameAnalysisRepository gameRepository = mock(GameAnalysisRepository.class);
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);

        TelegramChatEntity chat = new TelegramChatEntity(1L, "Ana", "ana", "private", false);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        UUID doneCorrelationId = UUID.randomUUID();
        GameHistoryDto doneDto = new GameHistoryDto(
                UUID.randomUUID(), doneCorrelationId,
                "opening-sgf", "middle-sgf", "end-sgf",
                "kitsune77", "opponent", "4k/5k"
        );
        when(gameRepository.findHistoryByChat(chat)).thenReturn(List.of(doneDto));

        AnalysisJobEntity doneJob = new AnalysisJobEntity(chat, Instant.now(), doneCorrelationId);
        setCreatedAt(doneJob, Instant.parse("2026-11-09T10:00:00Z"));
        when(jobRepository.findByCorrelationId(doneCorrelationId)).thenReturn(Optional.of(doneJob));

        AnalysisJobEntity waitingJob = new AnalysisJobEntity(chat, Instant.now(), UUID.randomUUID());
        waitingJob.setJobStatus(JobStatus.ANALYSIS_REQUESTED);
        setCreatedAt(waitingJob, Instant.parse("2026-11-10T09:00:00Z"));

        AnalysisJobEntity runningJob = new AnalysisJobEntity(chat, Instant.now(), UUID.randomUUID());
        runningJob.setJobStatus(JobStatus.IN_QUEUE);
        setCreatedAt(runningJob, Instant.parse("2026-11-10T08:00:00Z"));

        when(jobRepository.findByChatAndJobStatusIn(eq(chat),
                eq(EnumSet.of(JobStatus.ANALYSIS_REQUESTED, JobStatus.SUBMITTING, JobStatus.IN_QUEUE))))
                .thenReturn(List.of(waitingJob, runningJob));

        MiniAppService service = new MiniAppService(gameRepository, chatRepository, jobRepository, mock(JobCancellationService.class));

        List<GameHistoryItemDto> result = service.findAllByTelegramUserId(1L);

        assertEquals(3, result.size());
        assertEquals("WAITING", result.get(0).status());
        assertEquals("RUNNING", result.get(1).status());
        assertEquals("DONE", result.get(2).status());

        GameHistoryItemDto doneItem = result.get(2);
        assertEquals(doneDto.id(), doneItem.id());
        assertEquals("kitsune77", doneItem.blackPlayer());
        assertEquals("opening-sgf", doneItem.openingAnalysed());

        GameHistoryItemDto waitingItem = result.get(0);
        assertNull(waitingItem.openingAnalysed());
        assertNull(waitingItem.blackPlayer());
    }

    @Test
    void findAllByTelegramUserId_onMissingChat_throwsNotFound() {
        GameAnalysisRepository gameRepository = mock(GameAnalysisRepository.class);
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);

        when(chatRepository.findById(99L)).thenReturn(Optional.empty());

        MiniAppService service = new MiniAppService(gameRepository, chatRepository, jobRepository, mock(JobCancellationService.class));

        assertThrows(ResponseStatusException.class, () -> service.findAllByTelegramUserId(99L));
    }

    @Test
    void resignGame_onInProgressJob_delegatesToJobCancellationService() {
        GameAnalysisRepository gameRepository = mock(GameAnalysisRepository.class);
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
        JobCancellationService jobCancellationService = mock(JobCancellationService.class);

        TelegramChatEntity chat = new TelegramChatEntity(1L, "Ana", "ana", "private", false);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        AnalysisJobEntity job = new AnalysisJobEntity(chat, Instant.now(), UUID.randomUUID());
        job.setJobStatus(JobStatus.IN_QUEUE);
        UUID jobId = UUID.randomUUID();
        setId(job, jobId);

        when(jobRepository.findByIdAndChat(jobId, chat)).thenReturn(Optional.of(job));
        when(jobCancellationService.cancel(job)).thenReturn(true);

        MiniAppService service = new MiniAppService(gameRepository, chatRepository, jobRepository, jobCancellationService);

        service.resignGame(1L, jobId);

        verify(jobCancellationService).cancel(job);
        verify(gameRepository, never()).delete(any());
    }

    @Test
    void resignGame_onInProgressJob_whenCancellationFails_throwsBadGateway() {
        GameAnalysisRepository gameRepository = mock(GameAnalysisRepository.class);
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
        JobCancellationService jobCancellationService = mock(JobCancellationService.class);

        TelegramChatEntity chat = new TelegramChatEntity(1L, "Ana", "ana", "private", false);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        AnalysisJobEntity job = new AnalysisJobEntity(chat, Instant.now(), UUID.randomUUID());
        job.setJobStatus(JobStatus.IN_QUEUE);
        UUID jobId = UUID.randomUUID();
        setId(job, jobId);

        when(jobRepository.findByIdAndChat(jobId, chat)).thenReturn(Optional.of(job));
        when(jobCancellationService.cancel(job)).thenReturn(false);

        MiniAppService service = new MiniAppService(gameRepository, chatRepository, jobRepository, jobCancellationService);

        assertThrows(ResponseStatusException.class, () -> service.resignGame(1L, jobId));
    }

    @Test
    void resignGame_onDoneGame_deletesIt_withoutCallingJobCancellationService() {
        GameAnalysisRepository gameRepository = mock(GameAnalysisRepository.class);
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
        JobCancellationService jobCancellationService = mock(JobCancellationService.class);

        TelegramChatEntity chat = new TelegramChatEntity(1L, "Ana", "ana", "private", false);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        UUID gameId = UUID.randomUUID();
        when(jobRepository.findByIdAndChat(gameId, chat)).thenReturn(Optional.empty());

        GameAnalysisEntity game = mock(GameAnalysisEntity.class);
        when(gameRepository.findByIdAndChat(gameId, chat)).thenReturn(Optional.of(game));

        MiniAppService service = new MiniAppService(gameRepository, chatRepository, jobRepository, jobCancellationService);

        service.resignGame(1L, gameId);

        verify(gameRepository).delete(game);
        verify(jobCancellationService, never()).cancel(any());
    }

    @Test
    void resignGame_onUnknownId_throwsNotFound() {
        GameAnalysisRepository gameRepository = mock(GameAnalysisRepository.class);
        TelegramChatRepository chatRepository = mock(TelegramChatRepository.class);
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
        JobCancellationService jobCancellationService = mock(JobCancellationService.class);

        TelegramChatEntity chat = new TelegramChatEntity(1L, "Ana", "ana", "private", false);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        UUID unknownId = UUID.randomUUID();
        when(jobRepository.findByIdAndChat(unknownId, chat)).thenReturn(Optional.empty());
        when(gameRepository.findByIdAndChat(unknownId, chat)).thenReturn(Optional.empty());

        MiniAppService service = new MiniAppService(gameRepository, chatRepository, jobRepository, jobCancellationService);

        assertThrows(ResponseStatusException.class, () -> service.resignGame(1L, unknownId));
    }

    private static void setId(AnalysisJobEntity job, UUID id) {
        try {
            Field field = AnalysisJobEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(job, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
