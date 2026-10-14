package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fishing as it was: a catch is a raw fish and nothing else, and before 1.4.2 it granted no experience.
 *
 * <p>The loot half is ported from Nostalgic Tweaks {@code FishingHookMixin} (LGPL-3.0), which replaces the rolled
 * catch with one cod. minecraft.wiki, Fishing history: Alpha 1.2.0 "Players can only fish the Raw Fish item, rather
 * than any treasure or other loot", and Release 1.7.2 (13w36a) "Fishing rewards and mechanics have been massively
 * changed". Every declared version predates 1.7.2, so {@link HistoricalGameplayTweaks#oldFishingLoot()} is true for
 * all of them.
 *
 * <p>The experience half is NOT in upstream. Fishing XP arrives in 1.4.2 (12w37a): "Fishing now gives the player
 * experience with each fish caught", so earlier profiles must not spawn the orb. It is suppressed by
 * type rather than by call site: {@code retrieve} adds both the item entity and the orb through the same
 * {@code Level.addFreshEntity}, and testing the entity's class keeps the item untouched without depending on an
 * ordinal that a game update could reorder.
 */
@Mixin(FishingHook.class)
public abstract class FishingHookHistoricalMixin {
    @ModifyExpressionValue(
        method = "retrieve",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems("
                + "Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> historicalExperience$rawFishOnly(ObjectArrayList<ItemStack> caught) {
        if (!HistoricalGameplayTweaks.oldFishingLoot()) {
            return caught;
        }
        // A fresh list rather than clear-and-add: the rolled list belongs to the loot table's caller.
        ObjectArrayList<ItemStack> fish = new ObjectArrayList<>();
        fish.add(Items.COD.getDefaultInstance());
        return fish;
    }

    @WrapWithCondition(
        method = "retrieve",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean historicalExperience$noFishingExperience(Level level, Entity entity) {
        return !(entity instanceof ExperienceOrb && HistoricalGameplayTweaks.noFishingExperience());
    }
}
