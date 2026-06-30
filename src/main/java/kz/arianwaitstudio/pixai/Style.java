package kz.arianwaitstudio.pixai;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * Style configuration for a generation request (the v2 {@code style} object).
 *
 * <p><strong>Only supported by Tsubaki.2 models.</strong> The API accepts a named preset or a
 * custom style. The exact object schema is documented on the Models page rather than the
 * create-image page, so this is a flexible key/value wrapper: set whatever fields the API
 * expects via {@link #builder()} or {@link #of(Map)}.
 *
 * <pre>{@code
 * Style.builder().set("name", "anime").build();
 * }</pre>
 */
public final class Style {

    private final Map<String, Object> fields;

    private Style(Map<String, Object> fields) {
        this.fields = fields;
    }

    /** A style from a raw field map. */
    public static Style of(Map<String, Object> fields) {
        return new Style(new LinkedHashMap<>(fields));
    }

    public static Builder builder() {
        return new Builder();
    }

    JSONObject toJson() {
        return new JSONObject(fields);
    }

    /** Builder for {@link Style}. */
    public static final class Builder {
        private final Map<String, Object> fields = new LinkedHashMap<>();

        private Builder() {
        }

        /** Sets an arbitrary field on the style object. */
        public Builder set(String key, Object value) {
            fields.put(key, value);
            return this;
        }

        public Style build() {
            return new Style(new LinkedHashMap<>(fields));
        }
    }
}
