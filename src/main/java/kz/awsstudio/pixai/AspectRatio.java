package kz.awsstudio.pixai;

/**
 * Aspect ratio of the generated image (PixAI API v2 {@code aspectRatio}).
 *
 * <p>Supported by all model types. Defaults to {@link #RATIO_1_1} server-side when omitted.
 * The resulting pixel dimensions depend on the {@link Size} tier and can be resolved with
 * {@link #width(Size)} / {@link #height(Size)}.
 */
public enum AspectRatio {

    RATIO_1_1("1:1", 1024, 1024, 1536, 1536),
    RATIO_2_3("2:3", 848, 1280, 1024, 1536),
    RATIO_3_2("3:2", 1280, 848, 1536, 1024),
    RATIO_3_4("3:4", 864, 1152, 1152, 1536),
    RATIO_4_3("4:3", 1152, 864, 1536, 1152),
    RATIO_3_5("3:5", 768, 1280, 912, 1536),
    RATIO_5_3("5:3", 1280, 768, 1536, 912),
    RATIO_9_16("9:16", 720, 1280, 864, 1536),
    RATIO_16_9("16:9", 1280, 720, 1536, 864),
    RATIO_1_3("1:3", 512, 1536, 512, 1536),
    RATIO_3_1("3:1", 1536, 512, 1536, 512);

    private final String value;
    private final int width1k;
    private final int height1k;
    private final int width15k;
    private final int height15k;

    AspectRatio(String value, int width1k, int height1k, int width15k, int height15k) {
        this.value = value;
        this.width1k = width1k;
        this.height1k = height1k;
        this.width15k = width15k;
        this.height15k = height15k;
    }

    /** @return the wire value sent to the API, e.g. {@code "16:9"}. */
    public String value() {
        return value;
    }

    /**
     * @param size the resolution tier.
     * @return the resulting image width in pixels for this ratio at {@code size}.
     */
    public int width(Size size) {
        return size == Size.ONE_AND_A_HALF_K ? width15k : width1k;
    }

    /**
     * @param size the resolution tier.
     * @return the resulting image height in pixels for this ratio at {@code size}.
     */
    public int height(Size size) {
        return size == Size.ONE_AND_A_HALF_K ? height15k : height1k;
    }

    /**
     * @param value a ratio string such as {@code "16:9"}.
     * @return the matching constant.
     * @throws IllegalArgumentException if no constant matches.
     */
    public static AspectRatio fromValue(String value) {
        for (AspectRatio r : values()) {
            if (r.value.equals(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown aspect ratio: " + value);
    }
}
