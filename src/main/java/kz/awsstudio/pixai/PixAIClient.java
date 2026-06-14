package kz.awsstudio.pixai;

import java.time.Duration;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import kz.awsstudio.pixai.internal.GraphQLClient;
import okhttp3.OkHttpClient;

/**
 * Client for the <a href="https://pixai.art">PixAI</a> image-generation API.
 *
 * <p>Create one via the {@linkplain #builder() builder} (or the convenience
 * {@link #PixAIClient(String)} constructor), then call {@link #generate(GenerationParameters)}
 * for the common "submit, wait, fetch" flow, or use the lower-level
 * {@link #createTask}, {@link #getStatus}, {@link #awaitCompletion} and {@link #getMedia}
 * methods for finer control.
 *
 * <pre>{@code
 * PixAIClient client = PixAIClient.builder()
 *         .apiKey(System.getenv("PIXAI_API_KEY"))
 *         .build();
 *
 * GenerationParameters params = GenerationParameters.builder()
 *         .prompts("a fox in a forest, digital art")
 *         .build();
 *
 * GeneratedImage image = client.generate(params);
 * Path saved = image.saveTo(Paths.get("out"));
 * }</pre>
 *
 * <p>Instances are immutable and safe to share between threads. All methods throw
 * the unchecked {@link PixAIException} on API or transport failures.
 */
public final class PixAIClient {

    private static final String DEFAULT_ENDPOINT = "https://api.pixai.art/graphql";
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

    private static final String CREATE_TASK_MUTATION =
            "mutation createGenerationTask($parameters: JSONObject!) "
            + "{ createGenerationTask(parameters: $parameters) { id } }";
    private static final String GET_STATUS_QUERY =
            "query getTaskById($id: ID!) { task(id: $id) { id status } }";
    private static final String GET_OUTPUTS_QUERY =
            "query getTaskById($id: ID!) { task(id: $id) { outputs } }";
    private static final String GET_MEDIA_QUERY =
            "query getMediaById($id: String!) { media(id: $id) { urls { variant url } } }";

    private final GraphQLClient transport;
    private final Duration pollInterval;
    private final Duration timeout;

    private PixAIClient(Builder b) {
        OkHttpClient httpClient = b.httpClient != null ? b.httpClient : new OkHttpClient();
        this.transport = new GraphQLClient(httpClient, b.apiKey, b.baseUrl);
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
     * Submits a generation request, waits for it to finish and returns the image.
     *
     * @param params the generation parameters.
     * @return the generated image.
     * @throws PixAIException if the task fails, is cancelled, times out, or any
     *                        API call fails.
     */
    public GeneratedImage generate(GenerationParameters params) {
        String taskId = createTask(params);
        TaskStatus status = awaitCompletion(taskId);
        if (status != TaskStatus.COMPLETED) {
            throw new PixAIException("Generation task " + taskId
                    + " ended with status " + status);
        }
        return getMedia(taskId);
    }

    /**
     * Creates a generation task and returns its id without waiting for completion.
     *
     * @param params the generation parameters.
     * @return the task id.
     * @throws PixAIException if the request fails or the response is malformed.
     */
    public String createTask(GenerationParameters params) {
        Objects.requireNonNull(params, "params must not be null");
        JSONObject variables = new JSONObject();
        variables.put("parameters", params.toJson());

        JSONObject data = transport.query(CREATE_TASK_MUTATION, variables);
        try {
            return data.getJSONObject("createGenerationTask").getString("id");
        } catch (JSONException e) {
            throw new PixAIException("Unexpected createGenerationTask response: " + data, e);
        }
    }

    /**
     * Returns the current status of a task.
     *
     * @param taskId the task id.
     * @return the current {@link TaskStatus}.
     * @throws PixAIException if the request fails or the response is malformed.
     */
    public TaskStatus getStatus(String taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        JSONObject variables = new JSONObject();
        variables.put("id", taskId);

        JSONObject data = transport.query(GET_STATUS_QUERY, variables);
        try {
            return TaskStatus.fromApi(data.getJSONObject("task").getString("status"));
        } catch (JSONException e) {
            throw new PixAIException("Unexpected task status response: " + data, e);
        }
    }

    /**
     * Polls {@link #getStatus(String)} until the task reaches a terminal status or
     * the configured timeout elapses.
     *
     * @param taskId the task id.
     * @return the terminal {@link TaskStatus} (one of COMPLETED, FAILED, CANCELLED).
     * @throws PixAIException if the timeout elapses, the thread is interrupted, or a
     *                        status request fails.
     */
    public TaskStatus awaitCompletion(String taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            TaskStatus status = getStatus(taskId);
            if (status.isTerminal()) {
                return status;
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

    /**
     * Fetches the media produced by a completed task.
     *
     * @param taskId the task id.
     * @return the generated image.
     * @throws PixAIException if the task has no media or a request fails.
     */
    public GeneratedImage getMedia(String taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        String mediaId = fetchMediaId(taskId);
        String url = fetchMediaUrl(mediaId);
        return new GeneratedImage(mediaId, url, transport);
    }

    private String fetchMediaId(String taskId) {
        JSONObject variables = new JSONObject();
        variables.put("id", taskId);

        JSONObject data = transport.query(GET_OUTPUTS_QUERY, variables);
        try {
            JSONObject outputs = data.getJSONObject("task").getJSONObject("outputs");
            String mediaId = outputs.optString("mediaId", null);
            if (mediaId == null || mediaId.isEmpty()) {
                throw new PixAIException("Task " + taskId + " has no mediaId in outputs: " + outputs);
            }
            return mediaId;
        } catch (JSONException e) {
            throw new PixAIException("Unexpected task outputs response: " + data, e);
        }
    }

    private String fetchMediaUrl(String mediaId) {
        JSONObject variables = new JSONObject();
        variables.put("id", mediaId);

        JSONObject data = transport.query(GET_MEDIA_QUERY, variables);
        try {
            JSONArray urls = data.getJSONObject("media").getJSONArray("urls");
            for (int i = 0; i < urls.length(); i++) {
                String url = urls.getJSONObject(i).optString("url", null);
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            }
            throw new PixAIException("No downloadable URL for media " + mediaId);
        } catch (JSONException e) {
            throw new PixAIException("Unexpected media response: " + data, e);
        }
    }

    /**
     * @return a new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link PixAIClient}. */
    public static final class Builder {
        private String apiKey;
        private String baseUrl = DEFAULT_ENDPOINT;
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

        /** Overrides the GraphQL endpoint (defaults to {@value #DEFAULT_ENDPOINT}). */
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
