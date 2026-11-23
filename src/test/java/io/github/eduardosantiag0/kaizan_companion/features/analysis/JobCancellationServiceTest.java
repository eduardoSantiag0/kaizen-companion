package io.github.eduardosantiag0.kaizan_companion.features.analysis;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.AnalysisJobEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.JobStatus;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.TelegramChatEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.AnalysisJobRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobCancellationServiceTest {

    private static final TelegramChatEntity CHAT = new TelegramChatEntity(1L, "Ana", "ana", "private", false);

    @Test
    void cancel_onJobNeverSubmittedToRunPod_skipsRunPodCall_marksCancelledLocally() {
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
        RunPodCancelClient runPodCancelClient = mock(RunPodCancelClient.class);
        JobCancellationService service = new JobCancellationService(jobRepository, runPodCancelClient);

        AnalysisJobEntity job = new AnalysisJobEntity(CHAT, Instant.now(), UUID.randomUUID());
        job.setJobStatus(JobStatus.ANALYSIS_REQUESTED);
        // No runpodJobId: job never left our own pipeline, nothing to cancel on RunPod's side.

        boolean cancelled = service.cancel(job);

        assertTrue(cancelled);
        assertEquals(JobStatus.CANCELLED, job.getJobStatus());
        verify(jobRepository).save(job);
        verify(runPodCancelClient, never()).cancel(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancel_onJobAcceptedByRunPod_usesRunpodJobId_notCorrelationId() {
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
        RunPodCancelClient runPodCancelClient = mock(RunPodCancelClient.class);
        JobCancellationService service = new JobCancellationService(jobRepository, runPodCancelClient);

        AnalysisJobEntity job = new AnalysisJobEntity(CHAT, Instant.now(), UUID.randomUUID());
        job.setJobStatus(JobStatus.IN_QUEUE);
        job.setRunpodJobId("sync-9f2b1c-e1");

        when(runPodCancelClient.cancel("sync-9f2b1c-e1")).thenReturn(JobStatus.CANCELLED);

        boolean cancelled = service.cancel(job);

        assertTrue(cancelled);
        assertEquals(JobStatus.CANCELLED, job.getJobStatus());
        verify(jobRepository).save(job);
    }

    @Test
    void cancel_whenRunPodDoesNotConfirmCancellation_returnsFalse_andDoesNotMarkLocally() {
        AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
        RunPodCancelClient runPodCancelClient = mock(RunPodCancelClient.class);
        JobCancellationService service = new JobCancellationService(jobRepository, runPodCancelClient);

        AnalysisJobEntity job = new AnalysisJobEntity(CHAT, Instant.now(), UUID.randomUUID());
        job.setJobStatus(JobStatus.IN_QUEUE);
        job.setRunpodJobId("sync-9f2b1c-e1");

        when(runPodCancelClient.cancel("sync-9f2b1c-e1")).thenReturn(null);

        boolean cancelled = service.cancel(job);

        assertFalse(cancelled);
        assertEquals(JobStatus.IN_QUEUE, job.getJobStatus());
        verify(jobRepository, never()).save(job);
    }
}
