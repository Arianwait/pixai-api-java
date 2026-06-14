package kz.awsstudio.pixai;

/**
 * Quality/speed profile (PixAI API v2 {@code mode}).
 *
 * <p><strong>Only supported by Tsubaki.2 models.</strong> Defaults to {@link #STANDARD}
 * server-side when omitted.
 */
public enum Mode {

    LITE("lite"),
    STANDARD("standard"),
    PRO("pro"),
    ULTRA("ultra");

    private final String value;

    Mode(String value) {
        this.value = value;
    }

    /** @return the wire value sent to the API, e.g. {@code "standard"}. */
    public String value() {
        return value;
    }

    /**
     * @param value one of {@code lite}, {@code standard}, {@code pro}, {@code ultra}.
     * @return the matching constant.
     * @throws IllegalArgumentException if no constant matches.
     */
    public static Mode fromValue(String value) {
        for (Mode m : values()) {
            if (m.value.equals(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown mode: " + value);
    }
}
