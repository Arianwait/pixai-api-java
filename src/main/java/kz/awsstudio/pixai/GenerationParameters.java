package kz.awsstudio.pixai;

import org.json.JSONObject;

/**
 * Immutable set of parameters for a single image generation request.
 *
 * <p>Build instances with {@link #builder()}. Only the {@code prompts} field is
 * required; every other field is optional and is sent to the API only when it has
 * been explicitly set, mirroring the sparse-configuration behaviour of the original
 * client.
 *
 * <pre>{@code
 * GenerationParameters params = GenerationParameters.builder()
 *         .prompts("a fox in a forest, digital art")
 *         .negativePrompt("low quality")
 *         .modelId("1648918127446573124")
 *         .width(768).height(1280)
 *         .samplingSteps(50)
 *         .build();
 * }</pre>
 */
public final class GenerationParameters {

    private final String prompts;
    private final String negativePrompt;
    private final String modelId;
    private final String sampler;
    private final Integer width;
    private final Integer height;
    private final Integer samplingSteps;
    private final Double cfgScale;
    private final Double upscale;
    private final Boolean enableTile;

    private GenerationParameters(Builder b) {
        this.prompts = b.prompts;
        this.negativePrompt = b.negativePrompt;
        this.modelId = b.modelId;
        this.sampler = b.sampler;
        this.width = b.width;
        this.height = b.height;
        this.samplingSteps = b.samplingSteps;
        this.cfgScale = b.cfgScale;
        this.upscale = b.upscale;
        this.enableTile = b.enableTile;
    }

    public String getPrompts() {
        return prompts;
    }

    /**
     * Serialises these parameters into the JSON object expected by the PixAI
     * {@code createGenerationTask} mutation. Only fields that were set are included.
     * The JSON keys intentionally match the names the API expects
     * (e.g. {@code negative_prompt} is snake_case while the rest are camelCase).
     *
     * @return a new {@link JSONObject} representing these parameters.
     */
    JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("prompts", prompts);
        if (negativePrompt != null) json.put("negative_prompt", negativePrompt);
        if (modelId != null) json.put("modelId", modelId);
        if (sampler != null) json.put("sampler", sampler);
        if (width != null) json.put("width", width.intValue());
        if (height != null) json.put("height", height.intValue());
        if (samplingSteps != null) json.put("samplingSteps", samplingSteps.intValue());
        if (cfgScale != null) json.put("cfgScale", cfgScale.doubleValue());
        if (upscale != null) json.put("upscale", upscale.doubleValue());
        if (enableTile != null) json.put("enableTile", enableTile.booleanValue());
        return json;
    }

    /**
     * @return a new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link GenerationParameters}. */
    public static final class Builder {
        private String prompts;
        private String negativePrompt;
        private String modelId;
        private String sampler;
        private Integer width;
        private Integer height;
        private Integer samplingSteps;
        private Double cfgScale;
        private Double upscale;
        private Boolean enableTile;

        private Builder() {
        }

        /** The textual description of the image to generate (required). */
        public Builder prompts(String prompts) {
            this.prompts = prompts;
            return this;
        }

        /** Text describing what the image should avoid. */
        public Builder negativePrompt(String negativePrompt) {
            this.negativePrompt = negativePrompt;
            return this;
        }

        /** The PixAI model id to use. */
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        /** The sampler to use (e.g. {@code "Euler a"}). */
        public Builder sampler(String sampler) {
            this.sampler = sampler;
            return this;
        }

        /** Image width in pixels. */
        public Builder width(int width) {
            this.width = width;
            return this;
        }

        /** Image height in pixels. */
        public Builder height(int height) {
            this.height = height;
            return this;
        }

        /** Number of sampling steps. */
        public Builder samplingSteps(int samplingSteps) {
            this.samplingSteps = samplingSteps;
            return this;
        }

        /** CFG (classifier-free guidance) scale. */
        public Builder cfgScale(double cfgScale) {
            this.cfgScale = cfgScale;
            return this;
        }

        /** Upscale factor. */
        public Builder upscale(double upscale) {
            this.upscale = upscale;
            return this;
        }

        /** Whether to enable the Tile option. */
        public Builder enableTile(boolean enableTile) {
            this.enableTile = enableTile;
            return this;
        }

        /**
         * @return an immutable {@link GenerationParameters}.
         * @throws IllegalStateException if {@code prompts} was not set or is blank.
         */
        public GenerationParameters build() {
            if (prompts == null || prompts.trim().isEmpty()) {
                throw new IllegalStateException("prompts is required");
            }
            return new GenerationParameters(this);
        }
    }
}
