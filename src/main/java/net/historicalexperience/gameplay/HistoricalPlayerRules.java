package net.historicalexperience.gameplay;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Version-selected player movement and hunger rules.
 *
 * <p>Both are single booleans in a version's rule set, and both are enforced in exactly the same way
 * for every version: the hook asks the registry what this version does and then either lets the
 * vanilla behaviour run or stops it. That is the scalability point - supporting a new version is a
 * data change, not a new hook.
 *
 * <p><b>Sprinting.</b> Beta 1.7.3 had none; it arrives with the Adventure Update in Beta 1.8. The
 * server is authoritative, so cancelling {@code setSprinting(true)} is enough for the movement itself,
 * while client-side effects such as FOV are a presentation concern that follows from the player not
 * actually sprinting.
 *
 * <p><b>Hunger.</b> Beta 1.7.3 had no hunger system: no food-level drain, no saturation-driven health
 * regeneration and no starvation damage. All three live in {@code FoodData.tick}, so that is what a
 * no-hunger version stops, together with {@code addExhaustion}, which is the drain counter's only
 * entry point.
 *
 * <p><b>Known gap, deliberately not papered over.</b> Beta 1.7.3 healed <em>directly</em> when the
 * player ate. That is not implemented yet, so a no-hunger version currently has no hunger, no natural
 * regeneration and no starvation - but eating also does not yet heal. Direct food healing needs Beta's
 * per-item heal amounts verified from the client, which has not been done.
 */
public final class HistoricalPlayerRules {
    private static final LongAdder SPRINT_SUPPRESSIONS = new LongAdder();
    private static final LongAdder HUNGER_TICK_SUPPRESSIONS = new LongAdder();

    private HistoricalPlayerRules() {
    }

    /** True when this version allows sprinting. An unknown version is left with vanilla behaviour. */
    public static boolean allowsSprinting(String versionId) {
        return HistoricalVersions.byId(versionId)
            .map(version -> version.rules().allowsSprinting())
            .orElse(true);
    }

    /** True when this version has a hunger system. An unknown version keeps vanilla hunger. */
    public static boolean hasHunger(String versionId) {
        return HistoricalVersions.byId(versionId)
            .map(version -> version.rules().hasHunger())
            .orElse(true);
    }

    /** True when this version generates structures. An unknown version keeps vanilla structures. */
    public static boolean hasStructures(String versionId) {
        return HistoricalVersions.byId(versionId)
            .map(version -> version.rules().hasStructures())
            .orElse(true);
    }

    /** True when the version stops {@code FoodData.tick} entirely. */
    public static boolean suppressesHungerTick(String versionId) {
        return !hasHunger(versionId);
    }

    public static void recordSprintSuppression() {
        SPRINT_SUPPRESSIONS.increment();
    }

    public static void recordHungerTickSuppression() {
        HUNGER_TICK_SUPPRESSIONS.increment();
    }

    public static long sprintSuppressions() {
        return SPRINT_SUPPRESSIONS.sum();
    }

    public static long hungerTickSuppressions() {
        return HUNGER_TICK_SUPPRESSIONS.sum();
    }

    /** Which versions deny sprinting and which have no hunger, straight from the registry. */
    public static Map<String, String> diagnostics() {
        Map<String, String> report = new LinkedHashMap<>();
        for (HistoricalVersion version : HistoricalVersions.all()) {
            report.put(version.id(), "allowsSprinting=" + version.rules().allowsSprinting()
                + " hasHunger=" + version.rules().hasHunger());
        }
        report.put("sprintSuppressions", Long.toString(SPRINT_SUPPRESSIONS.sum()));
        report.put("hungerTickSuppressions", Long.toString(HUNGER_TICK_SUPPRESSIONS.sum()));
        return report;
    }
}
