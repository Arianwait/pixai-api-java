package kz.awsstudio.pixai;

import java.time.Duration;
import java.util.Objects;

import org.json.JSONObject;

import kz.awsstudio.pixai.internal.RestClient;
import okhttp3.OkHttpClient;

/**
 * Client for the <a href="https://pixai.art">PixAI</a> image-generation REST API (v2).
 *
 * <p>Create one via the {@linkplain #builder() builder} (or the convenience
 * {@link #PixAIClient(String)} constructor), then call {@link #generate(GenerationParameters)}
 * for the common "submit, wait, fetch" flow, or use the lower-level {@link #createTask},
 * {@link #getTask} and {@link #awaitCompletion} methods for finer control.
 *
 * <pre>{@code
 * PixAIClient client = PixAIClient.builder()
 *         .apiKey(System.getenv("PIXAI_API_KEY"))
 *         .build();
 *
 * GenerationParameters params = GenerationParameters.builder()
 *         .modelVersionId("1648918127446573124")
 *         .prompt("a fox in a forest, digital art")
 *         .build();
 *
 * Task task = client.generate(params);
 * task.images().get(0).saveTo(Paths.get("out"));
 * }</pre>
 *
 * <p>Instances are immutable and safe to share between threads. All methods throw the unchecked
 * {@link PixAIException} on API or transport failures.
 */
public final class PixAIClient {

    private static final String DEFAULT_BASE_URL = "https://api.pixai.art";
    private static final String CREATE_PATH = "/v2/image/create";
    private static final String TASK_PATH_PREFIX = "/v2/task/";

    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

    private final RestClient transport;
    private final Duration pollInterval;
    private final Duration timeout;

    private PixAIClient(Builder b) {
        OkHttpClient httpClient = b.httpClient != null ? b.httpClient : new OkHttpClient();
        this.transport = new RestClient(httpClient, b.apiKey, b.baseUrl);
        this.pollInterval = b.pollInterval;
        this.timeout = b.timeout;
    }

    /**
     * Convenience constructor using default endpoint, poll interval and timeout.
     *
     * @param apiKey the PixAI API key (must not be null or blank).
     */
    public PixAIClient(String apiKey) {
        this(builder().apiKey(apiKey));
    }

    /**
     * Submits a generation request, waits for it to finish and returns the completed task.
     *
     * @param params the generation parameters.
     * @return the completed {@link Task} (its {@link Task#images()} are populated).
     * @throws PixAIException if the task fails, is cancelled, times out, or any API call fails.
     */
    public Task generate(GenerationParameters params) {
        Task created = createTask(params);
        Task finished = awaitCompletion(created.getId());
        if (finished.getStatus() != TaskStatus.COMPLETED) {
            throw new PixAIException("Generation task " + finished.getId()
                    + " ended with status " + finished.getRawStatus());
        }
        return finished;
    }

    /**
     * Creates a generation task and returns it without waiting for completion.
     *
     * @param params the generation parameters.
     * @return the created {@link Task} (initially in a non-terminal status).
     * @throws PixAIException if the request fails or the response is malformed.
     */
    public Task createTask(GenerationParameters params) {
        Objects.requireNonNull(params, "params must not be null");
        JSONObject response = transport.post(CREATE_PATH, params.toJson());
        Task task = new Task(response, transport);
        if (task.getId() == null) {
            throw new PixAIException("create-image response had no task id: " + response);
        }
        return task;
    }

    /**
     * Fetches the current state of a task by id.
     *
     * @param taskId the task id.
     * @return the {@link Task} as it currently stands.
     * @throws PixAIException if the request fails or the response is malformed.
     */
    public Task getTask(String taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        JSONObject response = transport.get(TASK_PATH_PREFIX + taskId);
        return new Task(response, transport);
    }

    /**
     * Polls {@link #getTask(String)} until the task reaches a terminal status or the configured
     * timeout elapses.
     *
     * @param taskId the task id.
     * @return the {@link Task} in its terminal status.
     * @throws PixAIException if the timeout elapses, the thread is interrupted, or a request fails.
     */
    public Task awaitCompletion(String taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            Task task = getTask(taskId);
            if (task.getStatus().isTerminal()) {
                return task;
            }
            if (System.nanoTime() >= deadline) {
                throw new PixAIException("Timed out after " + timeout
                        + " waiting for task " + taskId);
            }
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PixAIException("Interrupted while waiting for task " + taskId, e);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link PixAIClient}. */
    public static final class Builder {
        private String apiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private Duration pollInterval = DEFAULT_POLL_INTERVAL;
        private Duration timeout = DEFAULT_TIMEOUT;
        private OkHttpClient httpClient;

        private Builder() {
        }

        /** The PixAI API key (required). */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /** Overrides the API base URL (defaults to {@value #DEFAULT_BASE_URL}). */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** How often to poll for task status while waiting (default 5s). */
        public Builder pollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
            return this;
        }

        /** Maximum time to wait for a task to finish (default 10 minutes). */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /** Supplies a custom {@link OkHttpClient} (e.g. for proxies, timeouts or tests). */
        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * @return a configured {@link PixAIClient}.
         * @throws IllegalStateException if required fields are missing or invalid.
         */
        public PixAIClient build() {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalStateException("apiKey is required");
            }
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalStateException("baseUrl must not be blank");
            }
            Objects.requireNonNull(pollInterval, "pollInterval must not be null");
            Objects.requireNonNull(timeout, "timeout must not be null");
            return new PixAIClient(this);
        }
    }
}
