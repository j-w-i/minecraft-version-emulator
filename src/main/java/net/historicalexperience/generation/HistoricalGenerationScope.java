package net.historicalexperience.generation;

/**
 * The generation recipe the running server is generating with, for hooks that cannot see a generator.
 *
 * <h2>Why a scope and not a lookup</h2>
 *
 * <p>Most generation rules resolve the version from the generator they were handed, which is exact and
 * cannot be wrong. Two cases have no generator to inspect:
 *
 * <ul>
 *   <li>a dimension whose level stem is an ordinary vanilla generator - the Nether and the End of a
 *       historical world are vanilla by design, so {@code historicalAlias} answers null there even though
 *       the world is historical and its structures still need the historical rules. Measured consequence
 *       before this class existed: a Beta 1.7.3 world could generate a Nether fortress, which is Release
 *       1.0.0 content;</li>
 *   <li>a hook called with only a registry access, where the world is known to the server but not to the
 *       call.</li>
 * </ul>
 *
 * <p>The scope is set from the server lifecycle: a historical world records its recipe alias, an ordinary
 * world records nothing, and a stopped server clears it. That ordering matters - the clearing is what
 * stops a historical alias from leaking into the next world loaded in the same process, which would make
 * an ordinary world start obeying historical structure rules.
 *
 * <p>It is deliberately <b>only</b> a fallback. A caller that has a generator must use it, because the
 * generator is per-dimension truth while this is per-server recollection.
 */
public final class HistoricalGenerationScope {
    private static volatile String generationAlias;

    private HistoricalGenerationScope() {
    }

    /** Records the running server's historical generation recipe, or null for an ordinary world. */
    public static void set(String alias) {
        generationAlias = (alias == null || alias.isBlank()) ? null : alias;
    }

    /** Forgets the scope. Called when the server stops. */
    public static void clear() {
        generationAlias = null;
    }

    /** The running server's historical generation recipe, or null when it is not a historical world. */
    public static String generationAlias() {
        return generationAlias;
    }
}
