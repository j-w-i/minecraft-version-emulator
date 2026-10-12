package net.historicalexperience.generation;

/**
 * The one place that answers "could this version have contained this block?".
 *
 * <h2>Two sources, one answer</h2>
 *
 * <p>Two tables can each prove a block is too new, and they cover different things:
 *
 * <ul>
 *   <li>{@link HistoricalBlockAvailability} dates a block to the earliest declared version that had it,
 *       derived from the reviewed per-version block-name sets and the feature timeline. This is the table
 *       that can say "Release 1.0.0 predates stone bricks", which is what makes a per-version judgement
 *       possible at all.</li>
 *   <li>{@link HistoricalBlockAnachronisms} lists blocks that are later than <b>every</b> declared version -
 *       copper, deepslate, the 1.13 ocean blocks, the 1.21 plants. These have no declared version to be
 *       dated to, so the introduction table cannot express them.</li>
 * </ul>
 *
 * <p>Asking both, in that order, means a block is only ever called an anachronism when at least one
 * reviewed table says so, and an unrecognised block stays UNKNOWN - counted, never treated as acceptable.
 * The direction matters: a wrong ANACHRONISTIC answer deletes content a version really had, which is worse
 * than a wrong UNKNOWN, which merely fails to report a defect.
 */
public final class HistoricalBlockContent {
    private HistoricalBlockContent() {
    }

    /** What the two tables together can say about one block in one version. */
    public static HistoricalBlockAvailability.Verdict judge(String versionId, String modernBlockId) {
        HistoricalBlockAvailability.Verdict dated =
            HistoricalBlockAvailability.judge(versionId, modernBlockId);
        if (dated != HistoricalBlockAvailability.Verdict.UNKNOWN) {
            return dated;
        }
        // Not dated to any declared version. If the version-agnostic list names the release that
        // introduced it, it postdates every declared version and is an anachronism here.
        return HistoricalBlockAnachronisms.introducedIn(modernBlockId) != null
            ? HistoricalBlockAvailability.Verdict.ANACHRONISTIC
            : HistoricalBlockAvailability.Verdict.UNKNOWN;
    }

    /** True only when a reviewed table positively says the version could not have had the block. */
    public static boolean isAnachronistic(String versionId, String modernBlockId) {
        return judge(versionId, modernBlockId) == HistoricalBlockAvailability.Verdict.ANACHRONISTIC;
    }

    /**
     * The release that introduced a block the version-agnostic list knows, for reporting. Null when that
     * list does not know the block; it is a label, never the verdict.
     */
    public static String introducedInRelease(String modernBlockId) {
        return HistoricalBlockAnachronisms.introducedIn(modernBlockId);
    }
}
