/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.EarlyGameplayRecipes;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disc/head drops are a separate custom-death path, not part of the replaceable gunpowder loot table. */
@Mixin(Creeper.class)
public abstract class CreeperHistoricalEarlyDropsMixin {
    /** Victims award skulls through several separate overrides. Refuse capability BEFORE their budget/item creation,
     * not by cancelling all victim custom loot (which would also destroy imported stored equipment).
     */
    @ModifyReturnValue(method = "canDropMobsSkull", at = @At("RETURN"), require = 1)
    private boolean historicalExperience$noEarlySkullAward(boolean original) {
        Creeper self = (Creeper) (Object) this;
        return original && !(self.level() instanceof ServerLevel level
            && EarlyGameplayRecipes.supports(HistoricalProfileLookup.profileIdForLevel(level)));
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$noEarlyDiscsOrHeads(ServerLevel level, DamageSource source,
            boolean recentlyHit, CallbackInfo ci) {
        if (EarlyGameplayRecipes.supports(HistoricalProfileLookup.profileIdForLevel(level))) ci.cancel();
    }
}
