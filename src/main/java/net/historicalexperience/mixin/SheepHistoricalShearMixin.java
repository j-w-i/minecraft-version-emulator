/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shears a sheep when it is <b>punched</b>, in the versions that had no shears - one of the oldest behaviours this
 * project reproduces, and the owner asked for it by name.
 *
 * <h2>The rule, read from the Alpha client</h2>
 *
 * <p>{@code alpha-1.1.2_01-client.jar} (sha1 {@code daa4b9f1…}, the exact jar for the {@code alpha} profile): the
 * sheep class is {@code bo}, it {@code extends ag} (the animal category), and it overrides
 * {@code attackEntityFrom(Entity, int)} - not any death-drop method:
 *
 * <pre>
 *   public boolean a(kh attacker, int damage) {
 *       if (!this.a) {                                  // the "shorn" flag
 *           if (attacker instanceof ge) {                // ge = EntityLiving
 *               this.a = true;
 *               int count = 1 + random.nextInt(3);        // 1 to 3 wool
 *               for (int i = 0; i < count; i++) dropItem(Item.cloth, 1, 1.0F);
 *           }
 *       }
 *       return super.attackEntityFrom(attacker, damage);
 *   }
 * </pre>
 *
 * <p>Two consequences, both reproduced here: punching shorns the sheep and yields <b>1 to 3</b> wool of its own
 * colour, and it happens <b>once</b> - the flag is what stops a second punch repeating it. The same class has no
 * drop override at all, so killing an Alpha sheep gave nothing; the shorn flag plus the colour-aware loot table in
 * {@link net.historicalexperience.gameplay.HistoricalMobLootHelper} already produce that outcome, which is why
 * nothing extra is needed for it.
 *
 * <h2>Why the hook is here and not on Sheep</h2>
 *
 * <p>{@code Sheep} does not override {@code hurt} in 1.21.1, so there is no method on it to inject into; the
 * inherited {@code LivingEntity.hurt} is the method a punch actually reaches. The handler therefore tests the
 * instance rather than being scoped by the target class. The shear happens at {@code HEAD}, as the original did,
 * so a killing blow still shorns first - that ordering is the jar's, not a convenience.
 *
 * <p>The gate is the version boundary and nothing else: shears are <b>Beta 1.7</b>, so every declared version
 * older than Beta 1.7.3 shears on a punch and Beta 1.7.3 itself does not.
 *
 * <p>A shared mixin: damage is authoritative on the server, but the client predicts it too and a disagreement
 * would show as a sheep that appears shorn locally and is not.
 */
@Mixin(LivingEntity.class)
public abstract class SheepHistoricalShearMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void historicalExperience$shearOnPunch(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> callback) {
        if (!((Object) this instanceof Sheep sheep)) {
            return;
        }

        if (!HistoricalGameplayTweaks.appliesTo("punchShear", HistoricalActiveSession.current())) {
            return;
        }

        if (sheep.isSheared()) {
            return;
        }

        // The jar's `attacker instanceof EntityLiving`: a player or a mob shorns it, fire and falling do not.
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)) {
            return;
        }

        // Server-side only for the DROP: the shorn flag travels through entity data, but an item entity created
        // on the client would be a ghost that vanishes on the next sync. The jar had no such split because its
        // client WAS the authority in singleplayer.
        if (sheep.level().isClientSide()) {
            return;
        }

        net.minecraft.world.level.ItemLike wool = SheepAccess.itemByDye().get(sheep.getColor());
        if (wool == null) {
            return;
        }

        sheep.setSheared(true);

        // `1 + random.nextInt(3)`, and one dropped stack rather than the jar's loop: the count and the colour are
        // the observable behaviour, and spawning them as a single stack of the same total is what a player sees.
        int count = 1 + sheep.getRandom().nextInt(3);
        sheep.spawnAtLocation(new ItemStack(wool, count));
    }
}
