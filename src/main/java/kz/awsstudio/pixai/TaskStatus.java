package kz.awsstudio.pixai;

/**
 * Status of a PixAI generation task.
 *
 * <p>The PixAI API returns the status as a free-form string. {@link #fromApi(String)}
 * maps the known values to enum constants and falls back to {@link #UNKNOWN} for any
 * value the library does not recognise, so a new server-side status never breaks
 * polling (it is simply treated as non-terminal).
 */
public enum TaskStatus {

    /** The task has been accepted but processing has not started yet. */
    PENDING(false),
    /** The task is currently being processed. */
    PROCESSING(false),
    /** The task finished successfully and media is available. */
    COMPLETED(true),
    /** The task failed. */
    FAILED(true),
    /** The task was cancelled. */
    CANCELLED(true),
    /** An unrecognised status; treated as non-terminal so polling continues. */
    UNKNOWN(false);

    private final boolean terminal;

    TaskStatus(boolean terminal) {
        this.terminal = terminal;
    }

    /**
     * @return {@code true} if no further status changes are expected
     *         (the task completed, failed or was cancelled).
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Maps a raw status string from the API to a {@link TaskStatus}.
     *
     * @param status the status string returned by the API (case-insensitive).
     * @return the matching constant, or {@link #UNKNOWN} if {@code null} or unrecognised.
     */
    public static TaskStatus fromApi(String status) {
        if (status == null) {
            return UNKNOWN;
        }
        switch (status.toLowerCase()) {
            case "pending":
            case "waiting":
                return PENDING;
            case "processing":
            case "running":
                return PROCESSING;
            case "completed":
            case "success":
                return COMPLETED;
            case "failed":
            case "error":
                return FAILED;
            case "cancelled":
            case "canceled":
                return CANCELLED;
            default:
                return UNKNOWN;
        }
    }
}
