package kz.arianwaitstudio.pixai;

/**
 * Recommended PixAI models, with their {@code modelVersionId} and {@link ModelType}.
 *
 * <p>Use these for convenience and to enable model-aware validation of parameters
 * (see {@link GenerationParameters}). Any other model can still be used by passing its
 * version id directly to {@link GenerationParameters.Builder#modelVersionId(String)}.
 *
 * <p>{@link #TSUBAKI_2} is the library default when no model is specified.
 */
public enum Model {

    /** Tsubaki.2 (DiT) — strong prompt understanding, accurate anatomy, multi-character scenes. */
    TSUBAKI_2("1983308862240288769", ModelType.DIT),
    /** Haruka v2 (SDXL) — stable quality, refined details, accurate hands. */
    HARUKA_V2("1861558740588989558", ModelType.SDXL),
    /** Hoshino v2 (SDXL) — very popular Japanese style. */
    HOSHINO_V2("1954632828118619567", ModelType.SDXL);

    /** The default model used when a request does not specify one. */
    public static final Model DEFAULT = TSUBAKI_2;

    private final String versionId;
    private final ModelType type;

    Model(String versionId, ModelType type) {
        this.versionId = versionId;
        this.type = type;
    }

    /** @return the {@code modelVersionId} to send in a request. */
    public String versionId() {
        return versionId;
    }

    /** @return the model architecture/type. */
    public ModelType type() {
        return type;
    }
}
