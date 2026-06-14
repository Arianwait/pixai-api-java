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

    private static final GenerationParameters PARAMS = GenerationParameters.builder()
            .modelVersionId("ver-1")
            .prompt("a fox")
            .build();

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = PixAIClient.builder()
                .apiKey("test-key")
                .baseUrl(server.url("/").toString())
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

        // POST /v2/image/create
        server.enqueue(json("{\"id\":\"task-1\",\"status\":\"waiting\"}").setResponseCode(201));
        // GET /v2/image/task-1  (poll: processing, then completed with outputs)
        server.enqueue(json("{\"id\":\"task-1\",\"status\":\"processing\"}"));
        server.enqueue(json("{\"id\":\"task-1\",\"status\":\"completed\",\"outputs\":{"
                + "\"mediaIds\":[\"m1\"],\"mediaUrls\":[\"" + downloadUrl + "\"]}}"));
        // GET download
        server.enqueue(new MockResponse().setBody("fake-image-bytes"));

        Task task = client.generate(PARAMS);

        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertEquals(1, task.images().size());
        GeneratedImage image = task.images().get(0);
        assertEquals("m1", image.getMediaId());
        assertEquals(downloadUrl, image.getUrl());
        assertEquals("fake-image-bytes", new String(image.getBytes(), StandardCharsets.UTF_8));

        // first request creates the task with auth and the right body
        RecordedRequest create = server.takeRequest();
        assertEquals("POST", create.getMethod());
        assertEquals("/v2/image/create", create.getPath());
        assertEquals("Bearer test-key", create.getHeader("Authorization"));
        assertTrue(create.getBody().readUtf8().contains("modelVersionId"));

        // second request polls the task by id
        RecordedRequest poll = server.takeRequest();
        assertEquals("GET", poll.getMethod());
        assertEquals("/v2/image/task-1", poll.getPath());
    }

    @Test
    void usesDefaultModelWhenNoneSpecified() throws Exception {
        server.enqueue(json("{\"id\":\"task-1\",\"status\":\"waiting\"}").setResponseCode(201));
        server.enqueue(json("{\"id\":\"task-1\",\"status\":\"completed\",\"outputs\":{}}"));

        // only a prompt -> model defaults to Tsubaki.2
        Task task = client.generate(GenerationParameters.builder().prompt("a fox").build());

        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        RecordedRequest create = server.takeRequest();
        String body = create.getBody().readUtf8();
        assertTrue(body.contains(Model.TSUBAKI_2.versionId()),
                "default model should be Tsubaki.2, body was: " + body);
    }

    @Test
    void wrapsHttpErrorsInPixAIException() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("unauthorized"));

        PixAIException ex = assertThrows(PixAIException.class, () -> client.createTask(PARAMS));
        assertTrue(ex.getMessage().contains("401"));
    }

    @Test
    void failedTaskStatusThrows() {
        server.enqueue(json("{\"id\":\"task-1\",\"status\":\"waiting\"}").setResponseCode(201));
        server.enqueue(json("{\"id\":\"task-1\",\"status\":\"failed\"}"));

        PixAIException ex = assertThrows(PixAIException.class, () -> client.generate(PARAMS));
        assertTrue(ex.getMessage().contains("failed"));
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
