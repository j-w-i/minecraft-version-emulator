package net.historicalexperience.world.metadata;

/**
 * Raised when persisted historical metadata cannot be read or is not self-consistent.
 *
 * <p>Callers must treat this as fail-closed: a world whose metadata cannot be read is a world
 * whose historical state is unknown, and the safe response is to refuse to proceed rather than
 * to overwrite the stored document. Existing world content is always preserved.
 */
public class MetadataException extends Exception {
    private static final long serialVersionUID = 1L;

    public MetadataException(String message) {
        super(message);
    }

    public MetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
