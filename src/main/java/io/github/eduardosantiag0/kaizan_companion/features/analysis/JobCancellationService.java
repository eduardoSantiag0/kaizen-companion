package io.github.eduardosantiag0.kaizan_companion.features.analysis;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.AnalysisJobEntity;
import io.github.eduardosantiag0.kaizan_companion.domain.entities.JobStatus;
import io.github.eduardosantiag0.kaizan_companion.domain.repositories.AnalysisJobRepository;
import org.springframework.stereotype.Service;

@Service
public class JobCancellationService {

    private final AnalysisJobRepository jobRepository;
    private final RunPodCancelClient runPodCancelClient;

    public JobCancellationService(AnalysisJobRepository jobRepository, RunPodCancelClient runPodCancelClient) {
        this.jobRepository = jobRepository;
        this.runPodCancelClient = runPodCancelClient;
    }

    public boolean cancel(AnalysisJobEntity job) {
        String runpodJobId = job.getRunpodJobId();
        JobStatus status = runpodJobId != null ? runPodCancelClient.cancel(runpodJobId) : JobStatus.CANCELLED;

        if (status != JobStatus.CANCELLED) {
            return false;
        }

        job.setJobStatus(JobStatus.CANCELLED);
        jobRepository.save(job);
        return true;
    }
}
