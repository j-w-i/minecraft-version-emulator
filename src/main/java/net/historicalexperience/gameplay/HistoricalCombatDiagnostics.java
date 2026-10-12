package net.historicalexperience.gameplay;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * How many times each combat hook has actually run, so a rule that is correct but never called can be told
 * apart from one that is called and ignored.
 *
 * <p>This exists because the owner's report - cooldown, sweep and damage values all still modern - came with
 * a diagnostic proving the active session was set and every gate answered true. That rules out the gate. What
 * it cannot distinguish is "the hook never runs" from "the hook runs and changes nothing that matters", and
 * those need different fixes.
 */
public final class HistoricalCombatDiagnostics {
    private static final LongAdder COOLDOWN_ANSWERS = new LongAdder();
    private static final LongAdder SWEEP_CANCELLED = new LongAdder();
    private static final LongAdder DAMAGE_REWRITES = new LongAdder();
    private static final LongAdder ORBS_REFUSED = new LongAdder();

    private HistoricalCombatDiagnostics() {
    }

    public static void recordCooldownAnswer() {
        COOLDOWN_ANSWERS.increment();
    }

    public static void recordSweepCancel() {
        SWEEP_CANCELLED.increment();
    }

    public static void recordDamageRewrite() {
        DAMAGE_REWRITES.increment();
    }

    public static void recordOrbRefused() {
        ORBS_REFUSED.increment();
    }

    /** The counters, for /hex status. */
    public static Map<String, String> report() {
        Map<String, String> report = new LinkedHashMap<>();
        report.put("cooldownHookCalls", Long.toString(COOLDOWN_ANSWERS.sum()));
        report.put("sweepHookCancels", Long.toString(SWEEP_CANCELLED.sum()));
        report.put("damageHookRewrites", Long.toString(DAMAGE_REWRITES.sum()));
        report.put("orbsRefused", Long.toString(ORBS_REFUSED.sum()));
        return report;
    }
}
