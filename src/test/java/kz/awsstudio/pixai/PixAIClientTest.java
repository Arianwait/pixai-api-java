package kz.awsstudio.pixai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class PixAIClientTest {

    private MockWebServer server;
    private PixAIClient client;

    private static final GenerationParameters PARAMS =
            GenerationParameters.builder().prompts("a fox").build();

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = PixAIClient.builder()
                .apiKey("test-key")
                .baseUrl(server.url("/graphql").toString())
                .pollInterval(Duration.ofMillis(5))
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void generateRunsTheFullFlowAndDownloadsBytes() throws Exception {
        String downloadUrl = server.url("/download/img.png").toString();

        server.enqueue(json("{\"data\":{\"createGenerationTask\":{\"id\":\"task-1\"}}}"));
        server.enqueue(json("{\"data\":{\"task\":{\"id\":\"task-1\",\"status\":\"processing\"}}}"));
        server.enqueue(json("{\"data\":{\"task\":{\"id\":\"task-1\",\"status\":\"completed\"}}}"));
        server.enqueue(json("{\"data\":{\"task\":{\"outputs\":{\"mediaId\":\"media-1\"}}}}"));
        server.enqueue(json("{\"data\":{\"media\":{\"urls\":[{\"variant\":\"PUBLIC\",\"url\":\""
                + downloadUrl + "\"}]}}}"));
        server.enqueue(new MockResponse().setBody("fake-image-bytes"));

        GeneratedImage image = client.generate(PARAMS);

        assertEquals("media-1", image.getMediaId());
        assertEquals(downloadUrl, image.getUrl());
        assertEquals("fake-image-bytes",
                new String(image.getBytes(), StandardCharsets.UTF_8));

        // the first request authenticates and posts to the GraphQL endpoint
        RecordedRequest first = server.takeRequest();
        assertEquals("POST", first.getMethod());
        assertEquals("/graphql", first.getPath());
        assertEquals("Bearer test-key", first.getHeader("Authorization"));
    }

    @Test
    void wrapsGraphQlErrorsInPixAIException() {
        server.enqueue(json("{\"errors\":[{\"message\":\"something broke\"}]}"));

        PixAIException ex = assertThrows(PixAIException.class, () -> client.createTask(PARAMS));
        assertTrue(ex.getMessage().contains("something broke"));
    }

    @Test
    void wrapsHttpErrorsInPixAIException() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        PixAIException ex = assertThrows(PixAIException.class, () -> client.createTask(PARAMS));
        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    void failedTaskStatusThrows() {
        server.enqueue(json("{\"data\":{\"createGenerationTask\":{\"id\":\"task-1\"}}}"));
        server.enqueue(json("{\"data\":{\"task\":{\"id\":\"task-1\",\"status\":\"failed\"}}}"));

        PixAIException ex = assertThrows(PixAIException.class, () -> client.generate(PARAMS));
        assertTrue(ex.getMessage().contains("FAILED"));
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
