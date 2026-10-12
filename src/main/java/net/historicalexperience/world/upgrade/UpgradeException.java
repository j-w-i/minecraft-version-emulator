package net.historicalexperience.world.upgrade;

/**
 * Thrown when a generation upgrade or its recovery cannot proceed safely.
 *
 * <p>Checked on purpose. Every caller of this package is deciding whether to change a world that may
 * contain someone's builds, and a refusal has to be handled rather than silently swallowed. The
 * alternative - an unchecked exception - invites a caller to let an upgrade half-happen.
 */
public class UpgradeException extends Exception {
    private static final long serialVersionUID = 1L;

    public UpgradeException(String message) {
        super(message);
    }

    public UpgradeException(String message, Throwable cause) {
        super(message, cause);
    }
}
