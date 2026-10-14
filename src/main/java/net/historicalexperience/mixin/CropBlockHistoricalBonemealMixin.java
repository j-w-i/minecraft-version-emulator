package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.level.block.CropBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * One bone meal fully grows a crop, before Release 1.5.
 *
 * <p>Ported from Nostalgic Tweaks {@code mixin/tweak/gameplay/mechanics_farming/CropBlockMixin} (LGPL-3.0; see
 * THIRD_PARTY_NOTICES.md). Upstream answers a config flag; this answers the world's version.
 *
 * <p>Source: minecraft.wiki, Bone Meal history, Release 1.5 13w04a: "Bone meal has been significantly nerfed ...
 * Wheat, melon and pumpkin seeds, potatoes and carrots: one bone meal is now consumed for all seven stages of growth"
 * - which describes the NEW cost, and 13w05a then settled growth at 2-5 uses. Before 13w04a one application took a
 * plant to its final state. {@link HistoricalGameplayTweaks#instantBonemeal()} holds the boundary.
 *
 * <p>Beetroot and torchflower override {@code getBonemealAgeIncrease} themselves, so this does not reach them. That is
 * correct rather than an omission: both postdate every declared version.
 */
@Mixin(CropBlock.class)
public abstract class CropBlockHistoricalBonemealMixin {
    @Shadow
    public abstract int getMaxAge();

    @ModifyReturnValue(method = "getBonemealAgeIncrease", at = @At("RETURN"))
    private int historicalExperience$instantBonemeal(int ageIncrease) {
        return HistoricalGameplayTweaks.instantBonemeal() ? this.getMaxAge() : ageIncrease;
    }
}
