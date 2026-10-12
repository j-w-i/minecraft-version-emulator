package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Removes the attack indicator from under the crosshair, in versions that had no cooldown to indicate.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_player/GuiMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The slice, the target and the {@code AttackIndicatorStatus.OFF} answer
 * are upstream's.
 *
 * <h2>The owner's report, and why the server half did not cover it</h2>
 *
 * <p>"The sweep gui is there even tho the effect is not and the cooldown gui under the crossbar is there even tho
 * also not enabled". Correct on both counts, and the cause is that the cooldown and the sweep were removed on the
 * SERVER while their INDICATOR is drawn by the client - this file. A feature is not removed by making it have no
 * effect; the reading of its state is part of it.
 *
 * <p>One injection covers both readings because the crosshair indicator is a single element: it fills with attack
 * readiness, and when a sweep is available it becomes the sweep indicator instead. Forcing the option to
 * {@code OFF} removes the element, so neither can be drawn.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(Gui.class)
public abstract class GuiHistoricalAttackIndicatorMixin {
    /** Upstream: {@code nt_combat_player$modifyCrosshairAttackIndicator}. */
    @SuppressWarnings("unchecked")
    @ModifyExpressionValue(
        method = "renderCrosshair",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate("
                    + "Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;"
                    + "Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;"
                    + "Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;"
                    + "Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V")),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private <T> T historicalExperience$modifyCrosshairAttackIndicator(T indicatorStatus) {
        if (HistoricalGameplayTweaks.appliesTo("cooldown", HistoricalActiveSession.current())) {
            return (T) AttackIndicatorStatus.OFF;
        }
        return indicatorStatus;
    }
}
