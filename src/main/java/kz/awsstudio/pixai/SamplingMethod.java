package kz.awsstudio.pixai;

/**
 * Sampling methods accepted in the {@code sampling} object (SDXL / SD v1 models only).
 *
 * <p>The same set is available for the recommended SDXL models. Use {@link #value()} for the
 * exact wire string, or pass one to {@link Sampling.Builder#method(SamplingMethod)}.
 */
public enum SamplingMethod {

    EULER_A("Euler a"),
    EULER("Euler"),
    LMS("LMS"),
    HEUN("Heun"),
    DPM2_KARRAS("DPM2 Karras"),
    DPM2_A_KARRAS("DPM2 a Karras"),
    DDIM("DDIM"),
    DPMPP_2M_KARRAS("DPM++ 2M Karras"),
    DPMPP_2S_A_KARRAS("DPM++ 2S a Karras"),
    DPMPP_SDE_KARRAS("DPM++ SDE Karras"),
    DPMPP_2M_SDE_KARRAS("DPM++ 2M SDE Karras"),
    RESTART("Restart");

    private final String value;

    SamplingMethod(String value) {
        this.value = value;
    }

    /** @return the wire value, e.g. {@code "DPM++ 2M Karras"}. */
    public String value() {
        return value;
    }

    /**
     * @param value a sampling method string.
     * @return the matching constant.
     * @throws IllegalArgumentException if no constant matches.
     */
    public static SamplingMethod fromValue(String value) {
        for (SamplingMethod m : values()) {
            if (m.value.equals(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown sampling method: " + value);
    }
}
