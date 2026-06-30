package kz.arianwaitstudio.pixai;

/**
 * Architecture/type of a PixAI model. Determines which generation parameters apply:
 * {@code mode} and {@code style} are {@link #DIT}-only (Tsubaki.2), while {@code sampling}
 * applies to {@link #SDXL} / {@link #SD_V1} models.
 */
public enum ModelType {
    SDXL,
    DIT,
    SD_V1
}
