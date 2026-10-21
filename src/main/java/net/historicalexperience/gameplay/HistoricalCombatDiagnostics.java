/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.gameplay;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * How many times each diagnostic hook has actually run, so a rule that is correct but never called can be told
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
    private static final LongAdder BOAT_IMPACT_CALLS = new LongAdder();
    private static final LongAdder BOAT_IMPACT_BREAKS = new LongAdder();
    private static final LongAdder BREW_GATE_CALLS = new LongAdder();
    private static final LongAdder BREW_GATE_REFUSALS = new LongAdder();
    private static final LongAdder BREW_GATE_NO_PROFILE = new LongAdder();
    private static volatile String BREW_LAST_INGREDIENT = "-";
    private static volatile String BREW_LAST_RESULT = "-";

    private HistoricalCombatDiagnostics() {
    }

    public static void recordSweepCancel() {
        SWEEP_CANCELLED.increment();
    }

    public static void recordOrbRefused() {
        ORBS_REFUSED.increment();
    }

    public static void recordBoatImpactCall() {
        BOAT_IMPACT_CALLS.increment();
    }

    public static void recordBoatImpactBreak() {
        BOAT_IMPACT_BREAKS.increment();
    }

    public static void recordBrewGateCall() {
        BREW_GATE_CALLS.increment();
    }

    public static void recordBrewGateRefusal() {
        BREW_GATE_REFUSALS.increment();
    }

    public static void recordBrewGateNoProfile() {
        BREW_GATE_NO_PROFILE.increment();
    }

    /** Records what the gate actually saw, so a probe can read it from /hex status. */
    public static void recordBrewMix(String ingredientId, String resultId) {
        BREW_LAST_INGREDIENT = ingredientId;
        BREW_LAST_RESULT = resultId;
    }

    /** The counters, for /hex status. */
    public static Map<String, String> report() {
        Map<String, String> report = new LinkedHashMap<>();
        report.put("cooldownHookCalls", Long.toString(COOLDOWN_ANSWERS.sum()));
        report.put("sweepHookCancels", Long.toString(SWEEP_CANCELLED.sum()));
        report.put("damageHookRewrites", Long.toString(DAMAGE_REWRITES.sum()));
        report.put("orbsRefused", Long.toString(ORBS_REFUSED.sum()));
        report.put("boatImpactCalls", Long.toString(BOAT_IMPACT_CALLS.sum()));
        report.put("boatImpactBreaks", Long.toString(BOAT_IMPACT_BREAKS.sum()));
        report.put("brewGateCalls", Long.toString(BREW_GATE_CALLS.sum()));
        report.put("brewGateRefusals", Long.toString(BREW_GATE_REFUSALS.sum()));
        report.put("brewGateNoProfile", Long.toString(BREW_GATE_NO_PROFILE.sum()));
        report.put("brewLastIngredient", BREW_LAST_INGREDIENT);
        report.put("brewLastResult", BREW_LAST_RESULT);
        return report;
    }
}
