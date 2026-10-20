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
package net.historicalexperience.mixin.common;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalEnchanting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The enchanting table's PRE-Release-1.8 click, and the fix for the Prerelease 3 placeholder becoming nonsense later.
 *
 * <p>{@code research/minecraft-version-history.md} Part IV records three states, and the middle one is the trap this
 * class originally got wrong: <b>Beta 1.9 Prerelease 3 has the table, its interface and its glint, and it DID spend
 * levels - what it had no data for was the enchantment itself.</b> Beta 1.8 has no table at all. From Prerelease 4
 * enchanting is real: seventeen enchantments, numeric levels, levels spent.
 *
 * <h2>Why the click is reimplemented rather than adjusted</h2>
 *
 * <p>Vanilla 1.21.1 applies the enchantment inside the {@code ContainerLevelAccess.execute} callback and charges a
 * flat {@code option + 1} there. A mixin cannot reach inside a lambda's bytecode, so the two era rules that live in
 * that lambda - what is charged, and that lapis is not needed - cannot be expressed as a modification of it. The
 * historical path therefore performs the click itself. It runs ONLY for a historical world; vanilla's own click is
 * left exactly as written for everything else, which is what keeps an ordinary world untouched.
 *
 * <h2>The three differences from vanilla, all of them the era's</h2>
 *
 * <ol>
 *   <li><b>No lapis.</b> The slot is never read, so nothing is required and nothing is consumed. Lapis arrived as a
 *       secondary cost in the Release 1.8 overhaul (14w02a), which is the boundary in
 *       {@link HistoricalEnchanting#usesOldTableRules}.</li>
 *   <li><b>The level charged is the offer's own requirement.</b> Release 1.8 "reduced experience cost from a cost
 *       equal to the enchant level to a flat 1, 2, or 3", so the third option of a level-30 offer costs thirty. Here
 *       the requirement the player must meet and the amount taken are the same figure, which is that rule.</li>
 *   <li><b>An offer that yields nothing is not charged.</b> Prerelease 3 applied no real enchantment, so a click that
 *       produces no enchantment hands the levels back with {@code giveExperienceLevels} instead of taking them for an
 *       item with no glint. Vanilla refunds in the same way when its own list comes back empty.</li>
 * </ol>
 *
 * <p>Server-side: the click is handled by the server, and the client's copy of the menu is presentation.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuHistoricalEnchantingMixin implements EnchantmentMenuInvoker {
    private static final Logger LOGGER = LoggerFactory.getLogger("historical_experience/enchanting");

    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private DataSlot enchantmentSeed;
    @Shadow @Final private net.minecraft.util.RandomSource random;

    /** Vanilla: {@code clickMenuButton(Player, int)} returns whether the click did anything. */
    @Inject(
        method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void historicalExperience$clickForThisVersion(Player player, int id,
            CallbackInfoReturnable<Boolean> callback) {
        if (Features.active(Feature.NO_ENCHANTING)) {
            // Before Prerelease 3 there is no usable table. false is what vanilla returns when a click does nothing,
            // so the client shows no enchantment and no level is taken.
            callback.setReturnValue(false);
            return;
        }
        if (!HistoricalEnchanting.usesOldTableRules(ActiveVersion.current())) {
            // An ordinary world, and every version from Release 1.8 on: vanilla's own click runs untouched.
            return;
        }
        callback.setReturnValue(historicalExperience$enchantTheOldWay(player, id));
    }

    /** The pre-Release-1.8 click. See the class comment for why it is written out rather than redirected. */
    private boolean historicalExperience$enchantTheOldWay(Player player, int id) {
        int[] costs = ((EnchantmentMenu) (Object) this).costs;
        if (id < 0 || id >= costs.length) {
            LOGGER.warn("{} clicked an out-of-range enchantment option {}", player.getName().getString(), id);
            return false;
        }
        final ItemStack item = this.enchantSlots.getItem(0);
        if (costs[id] <= 0 || item.isEmpty()) {
            return false;
        }
        if (player.experienceLevel < costs[id] && !player.getAbilities().instabuild) {
            return false;
        }
        int charged = costs[id];
        this.access.execute((level, pos) -> {
            player.onEnchantmentPerformed(item, charged);
            this.enchantmentSeed.set((this.enchantmentSeed.get() + 1) % 1000 + 1);
            this.random.setSeed(this.enchantmentSeed.get() + id);
            java.util.List<EnchantmentInstance> offers =
                ((EnchantmentMenuInvoker) this).historicalExperience$enchantmentList(level.registryAccess(), item, id, charged);
            if (offers.isEmpty()) {
                // Prerelease 3's state, and any version whose permitted set cannot apply to this item: the level is
                // handed back rather than charged for nothing.
                player.giveExperienceLevels(charged);
                return;
            }
            if (item.is(Items.BOOK)) {
                ItemStack book = item.transmuteCopy(Items.ENCHANTED_BOOK);
                this.enchantSlots.setItem(0, book);
                for (EnchantmentInstance offer : offers) {
                    book.enchant(offer.enchantment, offer.level);
                }
            } else {
                for (EnchantmentInstance offer : offers) {
                    item.enchant(offer.enchantment, offer.level);
                }
            }
            if (item.isEmpty()) {
                this.enchantSlots.setItem(0, ItemStack.EMPTY);
            }
            this.enchantSlots.setChanged();
            ((EnchantmentMenu) (Object) this).slotsChanged(this.enchantSlots);
            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, item, charged);
            }
        });
        return true;
    }

    /**
     * Tells the interface the lapis slot is satisfied, so its buttons are not greyed out for a cost that no longer
     * exists. {@code getGoldCount} is what the screen compares against each option, so answering the largest option
     * is enough to enable all three without touching the screen.
     */
    @Inject(method = "getGoldCount", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$lapisIsNotACost(CallbackInfoReturnable<Integer> callback) {
        if (HistoricalEnchanting.usesOldTableRules(ActiveVersion.current())) {
            int highest = 0;
            for (int cost : ((EnchantmentMenu) (Object) this).costs) {
                highest = Math.max(highest, cost);
            }
            callback.setReturnValue(highest);
        }
    }
}
