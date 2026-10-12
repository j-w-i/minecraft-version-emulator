package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops a skeleton dropping its bow, in versions where it had none to drop.
 *
 * <p>DIRECT PORT of the skeleton half of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mob_drops/MobMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.OLD_SKELETON_DROPS}. The injection
 * point - a wrap on the {@code spawnAtLocation} call inside {@code dropCustomDeathLoot} - is upstream's, and so
 * is the idea: the bow is CUSTOM death loot, a separate drop from the table, so the loot-table substitution
 * cannot reach it and it needs this hook.
 *
 * <h2>The boundary, from the research rather than from guesswork</h2>
 *
 * <p>feature-timeline.json: "Skeletons dropped arrows from Indev 20100219; 0-2 bones were added in Beta 1.2.
 * The bow drop is a separate, much later behaviour (1.2.1 12w06a, 8.5%)". So the bow is refused through Release
 * 1.1 and allowed from Release 1.2.1 - the same boundary and the same declared proxy (RELEASE_1_2_5) as the
 * avoid-sun gate, which is why the gate table carries both.
 *
 * <p>Two of upstream's three flags are deliberately not ported. {@code OLD_STYLE_STRAY_DROPS} is unreachable -
 * strays are 1.10 and no declared profile has them - and the research makes no claim about a zombie pigman
 * refusing its held item, so {@code OLD_ZOMBIE_PIGMEN_DROPS} would be a guess. A default-off upstream flag is
 * NOT a reason to skip either one; the missing evidence is.
 */
@Mixin(Mob.class)
public abstract class MobHistoricalCustomLootMixin {
    /** Upstream: {@code nt_mob_drops$shouldDropCustomDeathLoot}, skeleton branch only. */
    @WrapWithCondition(
        method = "dropCustomDeathLoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;spawnAtLocation("
                + "Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private boolean historicalExperience$shouldDropCustomDeathLoot(Mob mob, ItemStack itemStack) {
        if (mob.getType() == EntityType.SKELETON
            && HistoricalGameplayTweaks.appliesTo("customDeathLoot", HistoricalActiveSession.current())) {
            return false;
        }
        return true;
    }
}
