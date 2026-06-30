package kz.arianwaitstudio.pixai;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import kz.arianwaitstudio.pixai.internal.RestClient;

/**
 * A generation task returned by the PixAI API v2.
 *
 * <p>Carries the task id, its {@link TaskStatus}, lifecycle timestamps and — once the task has
 * completed — the produced {@linkplain #images() images}. A task may produce more than one image.
 */
public final class Task {

    private final String id;
    private final String rawStatus;
    private final TaskStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant startedAt;
    private final Instant endAt;
    private final List<GeneratedImage> images;

    Task(JSONObject json, RestClient transport) {
        this.id = json.optString("id", null);
        this.rawStatus = json.optString("status", null);
        this.status = TaskStatus.fromApi(rawStatus);
        this.createdAt = parseInstant(json, "createdAt");
        this.updatedAt = parseInstant(json, "updatedAt");
        this.startedAt = parseInstant(json, "startedAt");
        this.endAt = parseInstant(json, "endAt");
        this.images = Collections.unmodifiableList(parseImages(json, transport));
    }

    /** @return the task id. */
    public String getId() {
        return id;
    }

    /** @return the parsed status. */
    public TaskStatus getStatus() {
        return status;
    }

    /** @return the raw status string exactly as returned by the API (e.g. {@code "waiting"}). */
    public String getRawStatus() {
        return rawStatus;
    }

    /** @return when the task was created, or {@code null} if not present/unparseable. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** @return when the task was last updated, or {@code null}. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** @return when processing started, or {@code null}. */
    public Instant getStartedAt() {
        return startedAt;
    }

    /** @return when the task ended, or {@code null}. */
    public Instant getEndAt() {
        return endAt;
    }

    /**
     * @return the generated images (empty until the task has completed). Never {@code null};
     *         the returned list is unmodifiable.
     */
    public List<GeneratedImage> images() {
        return images;
    }

    private static Instant parseInstant(JSONObject json, String key) {
        String value = json.optString(key, null);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static List<GeneratedImage> parseImages(JSONObject json, RestClient transport) {
        List<GeneratedImage> result = new ArrayList<>();
        JSONObject outputs = json.optJSONObject("outputs");
        if (outputs == null) {
            return result;
        }
        JSONArray urls = outputs.optJSONArray("mediaUrls");
        JSONArray ids = outputs.optJSONArray("mediaIds");
        if (urls == null) {
            return result;
        }
        for (int i = 0; i < urls.length(); i++) {
            String url = urls.optString(i, null);
            if (url == null || url.isEmpty()) {
                continue;
            }
            String mediaId = (ids != null && i < ids.length()) ? ids.optString(i, null) : null;
            result.add(new GeneratedImage(mediaId, url, transport));
        }
        return result;
    }
}
