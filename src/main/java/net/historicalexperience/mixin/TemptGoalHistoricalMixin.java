package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops animals following a player holding their food, in versions whose animals did not.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/animal_spawn/TemptGoalMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_ANIMAL_TEMPTING}.
 *
 * <p>The {@code instanceof AgeableMob} test is upstream's and is the second time it has mattered: this goal is
 * also used by mobs that are not animals, so without the test the tweak would stop more than its name says.
 * The shadowed {@code mob} field and the {@code @ModifyReturnValue} shape are upstream's too.
 */
@Mixin(TemptGoal.class)
public abstract class TemptGoalHistoricalMixin {
    @Shadow @Final protected PathfinderMob mob;

    /** Upstream: {@code nt_animal_spawn$modifyShouldFollow}. */
    @ModifyReturnValue(method = "shouldFollow", at = @At("RETURN"))
    private boolean historicalExperience$modifyShouldFollow(boolean shouldFollow) {
        if (this.mob instanceof AgeableMob
            && HistoricalGameplayTweaks.appliesTo("tempting", HistoricalActiveSession.current())) {
            return false;
        }
        return shouldFollow;
    }
}
