package net.historicalexperience.gameplay;

/**
 * Which historical version the running game is currently showing, if any.
 *
 * <h2>Why this exists rather than reading a level</h2>
 *
 * <p>Most gates in this project take a {@code Level} and resolve the version from it, which is correct
 * whenever the question is about a world. Some questions are not about a world at all: an item's attack
 * damage modifier is a property of the item stack, shared by the whole session, with no level anywhere in
 * reach. Nostalgic Tweaks answers those with a global config flag; this project answers them with the
 * session's active version, which is still per-world from the player's point of view - one world is loaded
 * at a time - while never touching an ordinary world, because an ordinary world never sets it.
 *
 * <p>Set on the client when the server pushes its version, and on the server when a player joins a world
 * whose version resolved. Cleared when the world goes away, so a later ordinary world is unaffected.
 */
public final class HistoricalActiveSession {
    private static volatile String versionId;

    private HistoricalActiveSession() {
    }

    /** Records the active version. A null or blank id is treated as "no historical world". */
    public static void set(String id) {
        versionId = (id == null || id.isBlank()) ? null : id;
    }

    /** Forgets the active version. Called when leaving a world or stopping a server. */
    public static void clear() {
        versionId = null;
    }

    /** The active version id, or null when the game is not in a historical world. */
    public static String current() {
        return versionId;
    }

    /** Whether a historical version is active. */
    public static boolean isHistorical() {
        return versionId != null;
    }
}
