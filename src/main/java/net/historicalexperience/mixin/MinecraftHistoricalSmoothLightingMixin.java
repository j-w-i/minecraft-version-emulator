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
package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Turns smooth lighting off in versions that had no such thing, by overriding the video setting itself.
 *
 * <h2>Source</h2>
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/candy/world_lighting/MinecraftMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code CandyTweak.DISABLE_SMOOTH_LIGHTING}. Upstream's
 * injection point and its logic are kept:
 *
 * <pre>
 * &#64;ModifyReturnValue(method = "useAmbientOcclusion", at = &#64;At("RETURN"))
 * return !DISABLE_SMOOTH_LIGHTING &amp;&amp; useAmbientOcclusion;
 * </pre>
 *
 * <p>Overriding the video setting rather than the renderer is upstream's choice and the right one: everything that
 * asks "is smooth lighting on" - ambient occlusion, the corner blending, the option's own state - gets the same
 * answer, so there is no path that renders smooth lighting because it read the setting instead of this method.
 *
 * <h2>Boundary</h2>
 *
 * <p>Smooth lighting arrived in <b>Beta 1.3</b> - minecraft.wiki, Light, History: <i>"1.3 - Smooth lighting engine
 * added with the help of MrMessiah and his 'BetterLight' mod."</i> - so before it there is nothing to be smooth and
 * this answers false for every earlier version.
 *
 * <p>This is NOT the same question as {@link HistoricalSmoothLighting}, which covers Beta 1.3 up to Beta 1.7.3 and
 * asks about the shape of the shading. This one is about whether the feature existed at all.
 *
 * <p>It also has nothing to do with Classic. The engine once called "Classic lighting" was Minecraft before Infdev,
 * which no declared version is; it was removed for that reason.
 *
 * <p>This is a CLIENT mixin and is registered under {@code client}: {@code Minecraft} is a client-only class, and a
 * common registration would make every dedicated-server launch fail with {@code ClassNotFoundException} - a defect
 * this project has already recorded once.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftHistoricalSmoothLightingMixin {
    /** The first build with smooth lighting, so every earlier one answers false below. */
    private static final String FIRST_SMOOTH_LIGHTING_BUILD = "historical_experience:beta_1_3";

    @ModifyReturnValue(method = "useAmbientOcclusion", at = @At("RETURN"), require = 1)
    private static boolean historicalExperience$disableSmoothLighting(boolean useAmbientOcclusion) {
        var version = HistoricalClientRules.activeVersion();
        if (version == null) {
            // Unknown is not "yes": an unknown version leaves the player's own setting alone.
            return useAmbientOcclusion;
        }
        boolean beforeSmooth = HistoricalVersions.byId(FIRST_SMOOTH_LIGHTING_BUILD)
            .map(first -> version.chronology() < first.chronology())
            .orElse(false);
        if (beforeSmooth) {
            // Before Beta 1.3 there was no smooth lighting, so the setting is answered false regardless of what the
            // player chose. The option itself is left alone rather than forced, so a player who set it keeps their
            // preference for the next world they open.
            return false;
        }
        return useAmbientOcclusion;
    }
}