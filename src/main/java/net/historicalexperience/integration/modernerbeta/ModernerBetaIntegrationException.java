package net.historicalexperience.integration.modernerbeta;

/**
 * Raised when the Moderner Beta integration cannot satisfy a request.
 *
 * <p>This is a hard failure by design: a historical world whose terrain preset cannot be resolved
 * must not silently fall back to vanilla generation or to a different Moderner Beta preset, since
 * either would produce terrain that does not match the world's persisted metadata.
 */
public class ModernerBetaIntegrationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ModernerBetaIntegrationException(String message) {
        super(message);
    }

    public ModernerBetaIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
