"""Port the last piece of instant eating: no re-equip animation after an instant meal.

Carrier: gameplay/food_health/MinecraftMixin (52 lines), which wraps the itemUsed call inside startUseItem and
refuses it for instantaneous edibles - the same call site the sword blocking port already wraps, for the same
reason: itemUsed is what nudges the held item into the equip animation.

Composition, checked rather than assumed: @WrapWithCondition handlers on one instruction are combined, so a second
one is additive. The two conditions are also mutually exclusive in practice - an item is either a sword being
blocked with or food - so no ordering question arises. That is the same reasoning that let the instant-eat
use-duration mixin sit beside the sword block's on getUseDuration.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

MIXIN = '''package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.historicalexperience.gameplay.HistoricalFoodHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Stops the held item doing its equip animation after being eaten instantly.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/food_health/MinecraftMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.INSTANT_EAT}. The slice, the ordinal
 * and the {@code itemUsed} target are upstream's.
 *
 * <h2>Why this is a separate file from the sword block's identical injection</h2>
 *
 * <p>Both refuse the same call, for the same underlying reason - the equip animation - but they are different
 * rules with different conditions, and the sword block's one already exists. {@code @WrapWithCondition} handlers
 * compose on one instruction, so this is additive rather than a conflict: the call happens only when neither rule
 * refuses it. The two conditions cannot both be about one item, since an item is not a blocked sword and a meal
 * at once, so no ordering question arises.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftHistoricalInstantEatMixin {
    @Shadow @Nullable public LocalPlayer player;

    /** Upstream: {@code nt_food_health$shouldSetItemAsUsed}. */
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

        return !HistoricalFoodHelper.isInstantaneousEdible(this.player.getItemInHand(hand));
    }
}
'''
(root / 'src/main/java/net/historicalexperience/mixin/MinecraftHistoricalInstantEatMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the instant eat equip-animation mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
document.setdefault('client', [])
if 'MinecraftHistoricalInstantEatMixin' not in document['client']:
    document['client'].append('MinecraftHistoricalInstantEatMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; client mixins =', len(document['client']))
