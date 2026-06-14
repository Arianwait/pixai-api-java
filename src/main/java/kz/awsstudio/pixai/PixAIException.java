package kz.awsstudio.pixai;

/**
 * Unchecked exception thrown when a call to the PixAI API fails.
 *
 * <p>This wraps lower-level failures (network {@link java.io.IOException},
 * non-2xx HTTP responses, GraphQL {@code errors} payloads and malformed
 * responses) so that callers of the library deal with a single, library-specific
 * exception type instead of a mix of checked and unchecked exceptions.
 */
public class PixAIException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PixAIException(String message) {
        super(message);
    }

    public PixAIException(String message, Throwable cause) {
        super(message, cause);
    }
}
