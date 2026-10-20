/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.gameplay;

import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The values and data-derived rules that belong with the Nostalgic Tweaks ports but are NOT windows on the timeline.
 *
 * <h2>What moved out of here</h2>
 *
 * <p>This class used to hold every per-version gate, as string-keyed switches and one predicate per behaviour, with
 * its own version arithmetic. Those are now {@link net.historicalexperience.version.Feature} constants, declared once
 * with their windows and evidence, and asked with {@link Features#active(Feature)}. What remains here is what a
 * window cannot express:
 *
 * <ul>
 *   <li>{@link #jumpExperienceBug}: an <em>amount</em>, drawn at random, that applies in one build.</li>
 *   <li>{@link #eraArrowSpeedPercent}: a constant of the era's bow.</li>
 *   <li>{@link #experienceOrbSpawnDisabled}: derived from the version's own {@code awardsExperience} rule and
 *       resolved from a <b>world</b>, not from the session.</li>
 * </ul>
 *
 * <p>The behaviours were ported from <b>Nostalgic Tweaks</b> (LGPL-3.0; see THIRD_PARTY_NOTICES.md), where each is a
 * global config flag. Here the version decides, which is the whole point of this project.
 */
public final class HistoricalGameplayTweaks {
    private HistoricalGameplayTweaks() {
    }

    /**
     * Experience orbs must not exist, in a version that awarded no experience.
     *
     * <p>Reads the version's own {@code awardsExperience} rule rather than a boundary of its own, so the two cannot
     * disagree. It resolves the version from the <b>level</b> (a {@code ServerLevel} only; a client level, which has no
     * generator, is an ordinary world), exactly as before the gates became features.
     */
    public static boolean experienceOrbSpawnDisabled(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        String versionId = HistoricalProfileLookup.profileIdForLevel(serverLevel);
        if (versionId == null) {
            return false;
        }
        return HistoricalVersions.byId(versionId)
            .map(version -> !version.rules().awardsExperience())
            .orElse(false);
    }

    /**
     * The share of a full charge the era's bow needed, as a percentage.
     *
     * <p>Nostalgic Tweaks' {@code ARROW_SPEED} defaults to 70, and upstream turns it into a charge window of
     * {@code 72000 - (value / 100 * 20)} ticks. The owner named the same figure. The value lives here rather than in
     * the mixin so the regression the gate protects - "no charge at all" versus "a shorter charge" - is expressed in
     * one place. Whether it applies is {@link Feature#ARROW_SPEED}.
     */
    public static int eraArrowSpeedPercent() {
        return 70;
    }

    /**
     * How much experience a jump grants under the Pre1 bug, or 0 when the bug does not apply.
     *
     * <p>1-100 per the research, drawn per jump, so this returns the amount for one jump rather than a fixed value.
     *
     * <p>The zero is the safe answer twice over: it is what an unknown version and every other build get, and it is
     * the value the hook refuses to act on, so a session that is not Pre1 cannot be handed orb-free experience by a
     * caller that forgot to check the gate first.
     */
    public static int jumpExperienceBug(RandomSource random) {
        return jumpExperienceBug(ActiveVersion.current(), random);
    }

    /**
     * The same answer for an explicitly named version.
     *
     * <p>This overload exists so the amount can be asserted against a version id rather than only against the
     * session, because the session is unset in a unit test. The range is the part worth pinning: 1-100 inclusive, drawn
     * fresh per jump.
     */
    public static int jumpExperienceBug(String versionId, RandomSource random) {
        return Features.active(Feature.JUMP_EXPERIENCE_BUG, versionId)
            ? 1 + random.nextInt(100)
            : 0;
    }
}