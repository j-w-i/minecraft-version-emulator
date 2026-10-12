package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops baby animals spawning naturally, in versions whose animals did not.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/animal_spawn/AgeableMobMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_BABY_ANIMAL_SPAWNING}.
 *
 * <p>Upstream modifies the {@code isShouldSpawnBaby()} call INSIDE {@code finalizeSpawn}, so natural spawning
 * simply never rolls a baby and every other part of spawn finalisation is untouched. My first version hooked the
 * method's return and forced {@code setBaby(false)} afterwards, which reaches the same visible outcome by
 * running the whole spawn first and then undoing part of it.
 */
@Mixin(AgeableMob.class)
public abstract class AgeableMobHistoricalSpawnMixin {
    /** Upstream: {@code nt_animal_spawn$modifyShouldSpawnBaby}. */
    @ModifyExpressionValue(
        method = "finalizeSpawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/AgeableMob$AgeableMobGroupData;isShouldSpawnBaby()Z"))
    private boolean historicalExperience$modifyShouldSpawnBaby(boolean shouldSpawnBaby) {
        return !HistoricalGameplayTweaks.appliesTo("babyAnimal", HistoricalActiveSession.current())
            && shouldSpawnBaby;
    }
}
