package kz.awsstudio.pixai;

/**
 * Resolution tier of the generated image (PixAI API v2 {@code size}).
 *
 * <p>Defaults to {@link #ONE_K} server-side when omitted.
 */
public enum Size {

    /** ~1 megapixel (1024×1024). */
    ONE_K("1k"),
    /** ~2.36 megapixels (1536×1536); long side capped at 1536 px. */
    ONE_AND_A_HALF_K("1.5k");

    private final String value;

    Size(String value) {
        this.value = value;
    }

    /** @return the wire value sent to the API, e.g. {@code "1k"}. */
    public String value() {
        return value;
    }

    /**
     * @param value {@code "1k"} or {@code "1.5k"}.
     * @return the matching constant.
     * @throws IllegalArgumentException if no constant matches.
     */
    public static Size fromValue(String value) {
        for (Size s : values()) {
            if (s.value.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown size: " + value);
    }
}
