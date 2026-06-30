package kz.arianwaitstudio.pixai;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * A single LoRA entry for a generation request (an item of the v2 {@code loras} array).
 *
 * <p>Each item references a LoRA <em>version</em> id (the last segment of the LoRA's URL).
 * A request may carry up to 5 of these.
 *
 * <p><strong>Note:</strong> the full {@code LoraItem} schema is not exhaustively documented;
 * this models the documented {@code modelVersionId} plus an (assumed) {@code weight}. For any
 * additional fields use {@link #builder()} / {@link #raw(Map)} to set them explicitly.
 *
 * <pre>{@code
 * Lora.of("795...");          // just the version id
 * Lora.of("795...", 0.8);     // with a weight
 * Lora.builder().set("modelVersionId", "795...").set("weight", 0.8).build();
 * }</pre>
 */
public final class Lora {

    private final Map<String, Object> fields;

    private Lora(Map<String, Object> fields) {
        this.fields = fields;
    }

    /** A LoRA referenced only by its version id. */
    public static Lora of(String modelVersionId) {
        return builder().set("modelVersionId", modelVersionId).build();
    }

    /** A LoRA with a version id and weight. */
    public static Lora of(String modelVersionId, double weight) {
        return builder().set("modelVersionId", modelVersionId).set("weight", weight).build();
    }

    /** A LoRA from a raw field map (full control over the serialized object). */
    public static Lora raw(Map<String, Object> fields) {
        return new Lora(new LinkedHashMap<>(fields));
    }

    public static Builder builder() {
        return new Builder();
    }

    JSONObject toJson() {
        return new JSONObject(fields);
    }

    /** Builder for {@link Lora}. */
    public static final class Builder {
        private final Map<String, Object> fields = new LinkedHashMap<>();

        private Builder() {
        }

        /** Sets an arbitrary field on the LoRA item. */
        public Builder set(String key, Object value) {
            fields.put(key, value);
            return this;
        }

        public Lora build() {
            return new Lora(new LinkedHashMap<>(fields));
        }
    }
}
