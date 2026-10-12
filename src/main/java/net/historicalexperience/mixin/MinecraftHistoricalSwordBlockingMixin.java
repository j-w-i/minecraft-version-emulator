package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.historicalexperience.gameplay.HistoricalSwordBlocking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Keeps the sword block still: no re-equip animation and no swing each time the stance is entered.
 *
 * <p>DIRECT PORT of the first two injections of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_player/MinecraftMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). Both slices, both ordinals and both targets are upstream's.
 *
 * <h2>The symptom this fixes, in the owner's words</h2>
 *
 * <p>With the stance working but these two missing, every right-click called
 * {@code ItemInHandRenderer.itemUsed}, which nudges the held item into the equip animation, and
 * {@code LocalPlayer.swing}. The owner reported it exactly: "it does the resume animation or like smooth
 * animation all the time after doing it once rather than the quick immediate animation". Upstream's own comment
 * on the second injection is "Prevents the reequipped animation after sword blocking", so this is not an
 * interpretation - it is the same bug, already named.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}: {@code Minecraft} is not loaded on a server.
 *
 * <h2>Two of upstream's injections deliberately not ported</h2>
 *
 * <p>Upstream also allows attacking while blocking, behind {@code ATTACK_WHILE_SWORD_BLOCKING}, and resets its
 * miss timer behind {@code DISABLE_MISS_TIMER}. Neither is ported: the first is a gameplay choice about whether a
 * block should survive a swing, and the research says nothing about it, so enabling it would be this project
 * deciding rather than porting. The second belongs to a different tweak with no evidence gathered for it yet.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftHistoricalSwordBlockingMixin {
    @Shadow @Nullable public LocalPlayer player;

    /** Upstream: {@code nt_combat_player$shouldSwingOnSwordBlock}. */
    @WrapWithCondition(
        method = "startUseItem",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/item/ItemStack;isItemEnabled("
                    + "Lnet/minecraft/world/flag/FeatureFlagSet;)Z")),
        at = @At(
            ordinal = 2,
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;swing("
                + "Lnet/minecraft/world/InteractionHand;)V"))
    private boolean historicalExperience$shouldSwingOnSwordBlock(LocalPlayer player, InteractionHand hand) {
        return !HistoricalSwordBlocking.isBlocking(player);
    }

    /** Upstream: {@code nt_combat_player$shouldSetItemAsUsed}. The re-equip animation itself. */
    @WrapWithCondition(
        method = "startUseItem",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/item/ItemStack;isItemEnabled("
                    + "Lnet/minecraft/world/flag/FeatureFlagSet;)Z")),
        at = @At(
            ordinal = 1,
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;itemUsed("
                + "Lnet/minecraft/world/InteractionHand;)V"))
    private boolean historicalExperience$shouldSetItemAsUsed(ItemInHandRenderer itemInHandRenderer,
            InteractionHand hand) {
        if (this.player == null) {
            return true;
        }
        return !HistoricalSwordBlocking.isBlocking(this.player);
    }
}
