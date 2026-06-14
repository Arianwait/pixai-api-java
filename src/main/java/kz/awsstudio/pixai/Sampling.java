package kz.awsstudio.pixai;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * Sampling configuration for a generation request (the v2 {@code sampling} object).
 *
 * <p><strong>Only supported by SDXL / SD v1 models.</strong> All fields are optional. The exact
 * field set (and the list of sampling methods) is documented on the Models page rather than the
 * create-image page, so this is a flexible key/value wrapper: set whatever fields the API expects
 * via {@link #builder()} or {@link #of(Map)}.
 *
 * <pre>{@code
 * Sampling.builder().set("steps", 25).set("cfgScale", 6).build();
 * }</pre>
 */
public final class Sampling {

    private final Map<String, Object> fields;

    private Sampling(Map<String, Object> fields) {
        this.fields = fields;
    }

    /** Sampling config from a raw field map. */
    public static Sampling of(Map<String, Object> fields) {
        return new Sampling(new LinkedHashMap<>(fields));
    }

    public static Builder builder() {
        return new Builder();
    }

    JSONObject toJson() {
        return new JSONObject(fields);
    }

    /** Builder for {@link Sampling}. */
    public static final class Builder {
        private final Map<String, Object> fields = new LinkedHashMap<>();

        private Builder() {
        }

        /** Sets an arbitrary field on the sampling object. */
        public Builder set(String key, Object value) {
            fields.put(key, value);
            return this;
        }

        /**
         * Sets the sampling method.
         *
         * <p>Note: the exact JSON key for the method inside the {@code sampling} object is not
         * fully documented; this uses {@code "samplingMethod"}. Override with {@link #set} if the
         * API expects a different key.
         */
        public Builder method(SamplingMethod method) {
            return set("samplingMethod", method.value());
        }

        public Sampling build() {
            return new Sampling(new LinkedHashMap<>(fields));
        }
    }
}
