package kz.awsstudio.pixai;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import kz.awsstudio.pixai.internal.RestClient;

/**
 * A single generated image (one entry of a {@link Task}'s outputs).
 *
 * <p>Holds the media identifier and download URL and provides convenience methods to fetch the
 * bytes ({@link #getBytes()}) or save them to disk ({@link #saveTo(Path)}). Downloading is lazy
 * (and cached) so the caller decides whether and when to fetch the data.
 */
public final class GeneratedImage {

    private static final String FILENAME_DATE_PATTERN = "yyyyMMdd_HHmmss";

    private final String mediaId;
    private final String url;
    private final RestClient transport;

    private byte[] cachedBytes;

    GeneratedImage(String mediaId, String url, RestClient transport) {
        this.mediaId = mediaId;
        this.url = url;
        this.transport = transport;
    }

    /** @return the PixAI media id, or {@code null} if the API did not provide one. */
    public String getMediaId() {
        return mediaId;
    }

    /** @return the URL the image can be downloaded from. */
    public String getUrl() {
        return url;
    }

    /**
     * Downloads (once, then caches) and returns the raw image bytes.
     *
     * @return the image bytes.
     * @throws PixAIException if the download fails.
     */
    public byte[] getBytes() {
        if (cachedBytes == null) {
            cachedBytes = transport.download(url);
        }
        return cachedBytes;
    }

    /**
     * Saves the image into {@code directory} using a timestamped default file name
     * ({@code picture_PixAI_yyyyMMdd_HHmmss.png}).
     *
     * @param directory the target directory (created if it does not exist).
     * @return the path the image was written to.
     * @throws PixAIException if the download fails.
     * @throws UncheckedIOException if the file cannot be written.
     */
    public Path saveTo(Path directory) {
        String timeStamp = new SimpleDateFormat(FILENAME_DATE_PATTERN).format(new Date());
        return saveTo(directory, "picture_PixAI_" + timeStamp + ".png");
    }

    /**
     * Saves the image into {@code directory} under {@code fileName}.
     *
     * @param directory the target directory (created if it does not exist).
     * @param fileName  the file name to write.
     * @return the path the image was written to.
     * @throws PixAIException if the download fails.
     * @throws UncheckedIOException if the file cannot be written.
     */
    public Path saveTo(Path directory, String fileName) {
        byte[] bytes = getBytes();
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(fileName);
            Files.write(target, bytes);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save image to " + directory, e);
        }
    }
}
