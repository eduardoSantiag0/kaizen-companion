package io.github.eduardosantiag0.kaizan_companion.features.analysis;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.JobStatus;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.dtos.runpod.RunPodCancelRequest;
import io.github.eduardosantiag0.kaizan_companion.features.analysis.dtos.runpod.RunPodCancelledResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class RunPodCancelClient {

    private static final String SECRET_HEADER = "X-Kaizen-Secret";
    private static final String ENDPOINT = "/cancel";

    private final RestTemplate rest = new RestTemplate();

    @Value("${katago.runpod.status.url}")
    private String runpodStatusUrl;

    @Value("${katago.runpod.header-secret}")
    private String runpodHeaderSecret;

    public JobStatus cancel(String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(SECRET_HEADER, runpodHeaderSecret);

        RunPodCancelledResponse response = rest.postForObject(
                runpodStatusUrl + ENDPOINT,
                new HttpEntity<>(new RunPodCancelRequest(correlationId), headers),
                RunPodCancelledResponse.class
        );

        return response == null ? null : response.status();
    }
}
