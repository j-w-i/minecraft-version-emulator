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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.historicalexperience.client.hud.HudElement;
import net.historicalexperience.client.hud.HudHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Builds the pre-hunger heads-up display: no hunger bar, no experience bar, and the armour and air icons moved
 * into the space the hunger bar used to occupy.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code fabric/src/main/java/mod/adrenix/nostalgic/fabric/mixin/tweak/candy/old_hud/GuiMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). Every injection point, slice, shift and wrap condition below is
 * upstream's, and the calls into HudHelper are upstream's too. The only change is the condition: a global
 * config flag became HudHelper.hideHungerBar()/hideExperienceBar(), which answer for this world's version.
 *
 * <p>Upstream's stamina render hook is not ported - stamina is a Nostalgic Tweaks feature this project does
 * not have - so the STAMINA branch of the element switch stays empty rather than drawing a meaningless bar.
 */
@Mixin(Gui.class)
public abstract class GuiHistoricalHudMixin {
    /** Upstream: nt_fabric_old_hud$shouldExperienceBarRender. */
    @ModifyExpressionValue(
        method = "isExperienceBarVisible",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasExperience()Z"))
    private boolean historicalExperience$shouldExperienceBarRender(boolean hasExperience) {
        return !HudHelper.hideExperienceBar() && hasExperience;
    }

    /** Upstream: nt_fabric_old_hud$shiftHudElements. */
    @Inject(
        method = "renderHotbarAndDecorations",
        at = @At(
            shift = At.Shift.AFTER,
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderItemHotbar("
                + "Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void historicalExperience$shiftHudElements(GuiGraphics graphics, DeltaTracker deltaTracker,
            CallbackInfo callback) {
        HudHelper.begin(graphics);

        if (HudHelper.hideExperienceBar()) {
            HudHelper.apply(graphics, HudElement.EXPERIENCE_BAR);
        }
    }

    /** Upstream: nt_fabric_old_hud$shiftVehicleHealth. */
    @Inject(
        method = "renderHotbarAndDecorations",
        at = @At(
            shift = At.Shift.BEFORE,
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderVehicleHealth("
                + "Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void historicalExperience$shiftVehicleHealth(GuiGraphics graphics, DeltaTracker deltaTracker,
            CallbackInfo callback) {
        if (HudHelper.hideHungerBar()) {
            HudHelper.apply(graphics, HudElement.VEHICLE_HEALTH);
        }
    }

    /** Upstream: nt_fabric_old_hud$endHudShift. */
    @Inject(
        method = "renderHotbarAndDecorations",
        at = @At(
            shift = At.Shift.AFTER,
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderVehicleHealth("
                + "Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void historicalExperience$endHudShift(GuiGraphics graphics, DeltaTracker deltaTracker,
            CallbackInfo callback) {
        HudHelper.end(graphics);
    }

    /** Upstream: nt_fabric_old_hud$modifyArmorElement. */
    @Inject(
        method = "renderPlayerHealth",
        at = @At(
            shift = At.Shift.AFTER,
            value = "CONSTANT",
            args = "stringValue=armor"))
    private void historicalExperience$modifyArmorElement(GuiGraphics graphics, CallbackInfo callback) {
        if (HudHelper.hideHungerBar()) {
            HudHelper.apply(graphics, HudElement.ARMOR);
        }
    }

    /** Upstream: nt_fabric_old_hud$shouldArmorSpriteRender. */
    @WrapWithCondition(
        method = "renderPlayerHealth",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderArmor(Lnet/minecraft/client/gui/GuiGraphics;"
                + "Lnet/minecraft/world/entity/player/Player;IIII)V"))
    private boolean historicalExperience$shouldArmorSpriteRender(GuiGraphics graphics, Player player, int y,
            int heartRows, int height, int x) {
        return !HudHelper.hideHungerBar();
    }

    /** Upstream: nt_fabric_old_hud$popPushArmorOffsets. */
    @Inject(
        method = "renderPlayerHealth",
        at = @At(
            shift = At.Shift.BEFORE,
            value = "CONSTANT",
            args = "stringValue=health"))
    private void historicalExperience$popPushArmorOffsets(GuiGraphics graphics, CallbackInfo callback) {
        if (HudHelper.hideHungerBar()) {
            HudHelper.pop(graphics);
        }
    }

    /** Upstream: nt_fabric_old_hud$preHideFoodRelatedElements. */
    @Inject(
        method = "renderPlayerHealth",
        at = @At(
            shift = At.Shift.AFTER,
            value = "CONSTANT",
            args = "stringValue=food"))
    private void historicalExperience$preHideFoodRelatedElements(GuiGraphics graphics, CallbackInfo callback) {
        if (HudHelper.hideHungerBar()) {
            HudHelper.apply(graphics, HudElement.FOOD);
        }
    }

    /** Upstream: nt_fabric_old_hud$shouldFoodSpriteRender. */
    @WrapWithCondition(
        method = "renderPlayerHealth",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderFood(Lnet/minecraft/client/gui/GuiGraphics;"
                + "Lnet/minecraft/world/entity/player/Player;II)V"))
    private boolean historicalExperience$shouldFoodSpriteRender(Gui gui, GuiGraphics graphics, Player player,
            int y, int x) {
        return !HudHelper.hideHungerBar();
    }

    /** Upstream: nt_fabric_old_hud$postHideFoodRelatedElements. */
    @Inject(
        method = "renderPlayerHealth",
        at = @At(
            shift = At.Shift.BEFORE,
            value = "CONSTANT",
            args = "stringValue=air"))
    private void historicalExperience$postHideFoodRelatedElements(GuiGraphics graphics, CallbackInfo callback) {
        if (HudHelper.hideHungerBar()) {
            HudHelper.pop(graphics);
        }
    }

    /** Upstream: nt_fabric_old_hud$modifyAirElement. */
    @Inject(
        method = "renderPlayerHealth",
        slice = @Slice(
            from = @At(
                value = "CONSTANT",
                args = "stringValue=air")),
        at = @At(
            shift = At.Shift.AFTER,
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;getVisibleVehicleHeartRows(I)I"))
    private void historicalExperience$modifyAirElement(GuiGraphics graphics, CallbackInfo callback,
            @Local(ordinal = 0) int offsetHeight) {
        HudHelper.apply(graphics, HudElement.AIR);
    }

    /** Upstream: nt_fabric_old_hud$shouldAirSpriteRender. */
    @WrapWithCondition(
        method = "renderPlayerHealth",
        slice = @Slice(
            from = @At(
                value = "CONSTANT",
                args = "stringValue=air")),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite("
                + "Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private boolean historicalExperience$shouldAirSpriteRender(GuiGraphics graphics, ResourceLocation sprite,
            int x, int y, int width, int height) {
        return !HudHelper.hideHungerBar();
    }

    /** Upstream: nt_fabric_old_hud$popPushAirOffsets. */
    @Inject(
        method = "renderPlayerHealth",
        slice = @Slice(
            from = @At(
                value = "CONSTANT",
                args = "stringValue=air")),
        at = @At(
            shift = At.Shift.BEFORE,
            value = "INVOKE",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V"))
    private void historicalExperience$popPushAirOffsets(GuiGraphics graphics, CallbackInfo callback) {
        if (HudHelper.hideHungerBar()) {
            HudHelper.pop(graphics);
        }
    }
}
