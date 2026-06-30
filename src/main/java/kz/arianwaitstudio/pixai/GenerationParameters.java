package kz.arianwaitstudio.pixai;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Immutable request body for the PixAI API v2 {@code POST /v2/image/create} endpoint.
 *
 * <p>Build instances with {@link #builder()}. Only {@code prompt} is required; the model defaults
 * to {@link Model#DEFAULT} (Tsubaki.2) and every other field is optional and is sent only when
 * explicitly set.
 *
 * <pre>{@code
 * // minimal — Tsubaki.2 by default:
 * GenerationParameters.builder().prompt("a fox in a forest").build();
 *
 * // fully configured:
 * GenerationParameters params = GenerationParameters.builder()
 *         .model(Model.HARUKA_V2)
 *         .prompt("a fox in a forest, digital art")
 *         .negativePrompt("low quality")
 *         .aspectRatio(AspectRatio.RATIO_16_9)
 *         .size(Size.ONE_K)
 *         .seed(42L)
 *         .build();
 * }</pre>
 *
 * <p>Some fields are model-dependent: {@code mode} and {@code style} apply only to Tsubaki.2
 * (DiT) models, and {@code sampling} only to SDXL / SD v1 models. When the model's
 * {@link ModelType} is known (set via {@link Builder#model(Model)} or the default), {@link
 * Builder#build()} validates these for compatibility; for a custom {@code modelVersionId} the
 * type is unknown and validation is skipped.
 */
public final class GenerationParameters {

    private final String modelVersionId;
    private final String modelId;
    private final String prompt;
    private final String negativePrompt;
    private final String aspectRatio;
    private final String size;
    private final String mode;
    private final Style style;
    private final List<Lora> loras;
    private final Long seed;
    private final Sampling sampling;
    private final Boolean promptHelper;
    private final String callbackUrl;

    private GenerationParameters(Builder b) {
        this.modelVersionId = b.modelVersionId;
        this.modelId = b.modelId;
        this.prompt = b.prompt;
        this.negativePrompt = b.negativePrompt;
        this.aspectRatio = b.aspectRatio;
        this.size = b.size;
        this.mode = b.mode;
        this.style = b.style;
        this.loras = b.loras;
        this.seed = b.seed;
        this.sampling = b.sampling;
        this.promptHelper = b.promptHelper;
        this.callbackUrl = b.callbackUrl;
    }

    public String getModelVersionId() {
        return modelVersionId;
    }

    public String getPrompt() {
        return prompt;
    }

    /**
     * Serialises these parameters into the JSON body expected by {@code POST /v2/image/create}.
     * Only fields that were set are included.
     *
     * @return a new {@link JSONObject} representing the request body.
     */
    JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("modelVersionId", modelVersionId);
        json.put("prompt", prompt);
        if (modelId != null) json.put("modelId", modelId);
        if (negativePrompt != null) json.put("negativePrompt", negativePrompt);
        if (aspectRatio != null) json.put("aspectRatio", aspectRatio);
        if (size != null) json.put("size", size);
        if (mode != null) json.put("mode", mode);
        if (style != null) json.put("style", style.toJson());
        if (seed != null) json.put("seed", seed.longValue());
        if (sampling != null) json.put("sampling", sampling.toJson());
        if (promptHelper != null) json.put("promptHelper", promptHelper ? "enable" : "disable");
        if (callbackUrl != null) json.put("callbackUrl", callbackUrl);
        if (loras != null && !loras.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (Lora lora : loras) {
                arr.put(lora.toJson());
            }
            json.put("loras", arr);
        }
        return json;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link GenerationParameters}. */
    public static final class Builder {
        private Model model;
        private String modelVersionId;
        private String modelId;
        private String prompt;
        private String negativePrompt;
        private String aspectRatio;
        private String size;
        private String mode;
        private Style style;
        private List<Lora> loras;
        private Long seed;
        private Sampling sampling;
        private Boolean promptHelper;
        private String callbackUrl;

        private Builder() {
        }

        /**
         * Selects a recommended {@link Model} (sets {@code modelVersionId} and enables
         * type-aware parameter validation).
         */
        public Builder model(Model model) {
            this.model = model;
            this.modelVersionId = model != null ? model.versionId() : null;
            return this;
        }

        /**
         * The model <em>version</em> id (last segment of the model URL). Overrides any
         * {@link #model(Model)} selection; the model type then becomes unknown so
         * compatibility validation is skipped. When neither is set, {@link Model#DEFAULT}
         * (Tsubaki.2) is used.
         */
        public Builder modelVersionId(String modelVersionId) {
            this.modelVersionId = modelVersionId;
            this.model = null;
            return this;
        }

        /**
         * The legacy model id.
         *
         * @deprecated the v2 API expects {@link #modelVersionId(String)}; this is kept only for
         *             callers migrating from older code.
         */
        @Deprecated
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        /** The main prompt. Required. */
        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /** What to exclude from the generation. */
        public Builder negativePrompt(String negativePrompt) {
            this.negativePrompt = negativePrompt;
            return this;
        }

        /** Aspect ratio (type-safe). Supported by all model types. */
        public Builder aspectRatio(AspectRatio aspectRatio) {
            this.aspectRatio = aspectRatio != null ? aspectRatio.value() : null;
            return this;
        }

        /** Aspect ratio as a raw wire value (e.g. {@code "16:9"}), for forward compatibility. */
        public Builder aspectRatio(String aspectRatio) {
            this.aspectRatio = aspectRatio;
            return this;
        }

        /** Resolution tier (type-safe). */
        public Builder size(Size size) {
            this.size = size != null ? size.value() : null;
            return this;
        }

        /** Resolution tier as a raw wire value (e.g. {@code "1k"}). */
        public Builder size(String size) {
            this.size = size;
            return this;
        }

        /** Quality/speed profile (type-safe). Tsubaki.2 models only. */
        public Builder mode(Mode mode) {
            this.mode = mode != null ? mode.value() : null;
            return this;
        }

        /** Quality/speed profile as a raw wire value. Tsubaki.2 models only. */
        public Builder mode(String mode) {
            this.mode = mode;
            return this;
        }

        /** Style configuration. Tsubaki.2 models only. */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /** Replaces the LoRA list (max 5). */
        public Builder loras(List<Lora> loras) {
            this.loras = loras != null ? new ArrayList<>(loras) : null;
            return this;
        }

        /** Adds a LoRA (max 5). */
        public Builder addLora(Lora lora) {
            if (this.loras == null) {
                this.loras = new ArrayList<>();
            }
            this.loras.add(lora);
            return this;
        }

        /** Adds a LoRA by its version id (max 5). */
        public Builder addLora(String loraVersionId) {
            return addLora(Lora.of(loraVersionId));
        }

        /** Seed for reproducibility (random when unset). */
        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        /** Sampling configuration. SDXL / SD v1 models only. */
        public Builder sampling(Sampling sampling) {
            this.sampling = sampling;
            return this;
        }

        /** Enables/disables the built-in prompt enhancer (enabled by default server-side). */
        public Builder promptHelper(boolean enabled) {
            this.promptHelper = enabled;
            return this;
        }

        /** Webhook URL that PixAI calls (POST) when the task status changes. */
        public Builder callbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        /**
         * @return an immutable {@link GenerationParameters}.
         * @throws IllegalStateException if {@code prompt} is missing/blank, more than 5 LoRAs are
         *                               provided, or a parameter is incompatible with a known
         *                               model type.
         */
        public GenerationParameters build() {
            if (prompt == null || prompt.trim().isEmpty()) {
                throw new IllegalStateException("prompt is required");
            }
            if (loras != null && loras.size() > 5) {
                throw new IllegalStateException("at most 5 loras are allowed, got " + loras.size());
            }

            // Resolve the model (defaulting to Tsubaki.2) and its type for compatibility checks.
            ModelType type;
            if (modelVersionId == null) {
                model = Model.DEFAULT;
                modelVersionId = Model.DEFAULT.versionId();
                type = Model.DEFAULT.type();
            } else if (model != null) {
                type = model.type();
            } else {
                type = null; // custom modelVersionId — type unknown, skip validation
            }

            if (type == ModelType.DIT && sampling != null) {
                throw new IllegalStateException(
                        "sampling is only supported by SDXL / SD v1 models, not " + model);
            }
            if (type == ModelType.SDXL || type == ModelType.SD_V1) {
                if (mode != null) {
                    throw new IllegalStateException(
                            "mode is only supported by Tsubaki.2 (DiT) models, not " + model);
                }
                if (style != null) {
                    throw new IllegalStateException(
                            "style is only supported by Tsubaki.2 (DiT) models, not " + model);
                }
            }
            return new GenerationParameters(this);
        }
    }
}
