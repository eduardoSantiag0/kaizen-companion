package io.github.eduardosantiag0.kaizan_companion.features.analysis;

import io.github.eduardosantiag0.kaizan_companion.domain.entities.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RunPodCancelClientTest {

    private static final String STATUS_URL = "http://status-api:8971";

    private record Fixture(RunPodCancelClient client, MockRestServiceServer server) {
    }

    private Fixture newClient() {
        RunPodCancelClient client = new RunPodCancelClient();
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        ReflectionTestUtils.setField(client, "rest", restTemplate);
        ReflectionTestUtils.setField(client, "runpodStatusUrl", STATUS_URL);
        ReflectionTestUtils.setField(client, "runpodHeaderSecret", "secret");

        return new Fixture(client, server);
    }

    @Test
    void cancel_on404FromStatusApi_treatsItAsAlreadyFinished_returnsCancelled() {
        Fixture fixture = newClient();
        fixture.server().expect(requestTo(STATUS_URL + "/cancel"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Job não encontrado\"}"));

        JobStatus status = fixture.client().cancel("sync-7b963b87-a81d-4a3b-9613-ee95f670ee64-u2");

        assertEquals(JobStatus.CANCELLED, status);
    }

    @Test
    void cancel_onSuccessfulResponse_returnsItsStatus() {
        Fixture fixture = newClient();
        fixture.server().expect(requestTo(STATUS_URL + "/cancel"))
                .andRespond(withSuccess(
                        "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"status\":\"CANCELLED\"}",
                        MediaType.APPLICATION_JSON));

        JobStatus status = fixture.client().cancel("sync-9f2b1c-e1");

        assertEquals(JobStatus.CANCELLED, status);
    }

    @Test
    void cancel_onOtherError_returnsNull() {
        Fixture fixture = newClient();
        fixture.server().expect(requestTo(STATUS_URL + "/cancel"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        JobStatus status = fixture.client().cancel("sync-9f2b1c-e1");

        assertNull(status);
    }
}
