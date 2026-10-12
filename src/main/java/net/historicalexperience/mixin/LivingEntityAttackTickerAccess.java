package net.historicalexperience.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code LivingEntity.attackStrengthTicker}.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks' {@code LivingEntityAccess} usage in
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_player/PlayerMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which calls {@code nt$setAttackStrengthTicker} from the attack
 * hook to disable the cooldown.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAttackTickerAccess {
    @Accessor("attackStrengthTicker")
    void setAttackStrengthTicker(int value);
}
