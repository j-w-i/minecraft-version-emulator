package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The player's air refills the instant their head breaks the surface, rather than trickling back.
 *
 * <h2>Source</h2>
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mechanics_swim/LivingEntityMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.INSTANT_AIR}. Upstream's injection
 * point and its two guards are kept verbatim:
 *
 * <pre>
 * &#64;ModifyReturnValue(method = "increaseAirSupply", at = &#64;At("RETURN"))
 * if (this.getType() == EntityType.PLAYER)
 *     return INSTANT_AIR ? this.getMaxAirSupply() : currentAir;
 * </pre>
 *
 * <p><b>Only a player is affected.</b> That check is upstream's and it is kept: other living entities that breathe
 * air - and every entity's drowning timer - behave exactly as before, so this changes the player's experience of
 * surfacing and nothing else about the world. Removing the guard "for simplicity" would silently change mob
 * behaviour too.
 *
 * <p>The version comes from the world rather than a config flag, which is the whole point of the port:
 * {@link HistoricalGameplayTweaks#instantAir()} is true before Beta 1.8 and false from it, the same boundary the
 * rest of Nostalgic Tweaks' swimming group uses.
 *
 * <p>Shared mixin: air supply is player state and the server owns it. A client-side copy would be prediction, and
 * predicting an air bar the server disagrees with is the desync this project avoids elsewhere.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalInstantAirMixin {
    @ModifyReturnValue(method = "increaseAirSupply", at = @At("RETURN"))
    private int historicalExperience$instantAirRefill(int currentAir) {
        if (!((Object) this instanceof LivingEntity livingEntity)
                || livingEntity.getType() != EntityType.PLAYER) {
            return currentAir;
        }
        return HistoricalGameplayTweaks.instantAir() ? livingEntity.getMaxAirSupply() : currentAir;
    }
}
