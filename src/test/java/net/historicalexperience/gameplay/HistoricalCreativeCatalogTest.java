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
package net.historicalexperience.gameplay;

import java.util.List;
import net.historicalexperience.client.HistoricalCreativeInventory;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real vanilla registries/components, not Fabric packet/mixin execution. */
class HistoricalCreativeCatalogTest {
    private static final String P = "historical_experience:", M = "minecraft:";
    private static HolderLookup.Provider registries;
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); registries = VanillaRegistries.createLookup(); }
    private static boolean has(String version, String item) { return HistoricalCreativeCatalog.available(P + version, M + item); }
    private static boolean grants(String version, ItemStack stack) { return HistoricalCreativeGrants.allowed(P + version, stack); }
    @Test void everyDatedIdActuallyExistsAndWindowsAreValid() {
        assertTrue(HistoricalCreativeCatalog.all().size() > 300);
        HistoricalCreativeCatalog.all().forEach((id, window) -> {
            assertNotEquals(Items.AIR, BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)), id);
            assertTrue(window.first() < window.endExclusive(), id);
        });
        assertThrows(UnsupportedOperationException.class, () -> HistoricalCreativeCatalog.all().clear());
    }
    @Test void allHistoricalProfilesHaveBasicBlocksButNoUnknownFutureOrModItems() {
        for (var version : HistoricalVersions.all()) {
            for (String item : List.of("stone", "dirt", "cobblestone", "oak_planks", "diamond_pickaxe")) assertTrue(HistoricalCreativeCatalog.available(version.id(), M + item), version.id() + item);
            for (String item : List.of("netherite_block", "copper_block", "bamboo_planks", "cherry_log", "crossbow", "shield", "nether_wart_block", "white_bed", "blue_dye", "spawner", "dragon_egg", "smooth_stone", "oak_wood")) assertFalse(HistoricalCreativeCatalog.available(version.id(), M + item), version.id() + item);
            assertFalse(HistoricalCreativeCatalog.available(version.id(), "another_mod:unknown_block"));
        }
    }
    @Test void ordinaryUnknownAndRecipeAliasesRemainUnaffected() {
        for (String selection : new String[]{null, "", "minecraft:normal", "unknown", P + "beta_1_7_3_content_preview_10"}) {
            assertTrue(HistoricalCreativeCatalog.available(selection, M + "netherite_block"));
            assertTrue(HistoricalCreativeGrants.allowed(selection, new ItemStack(Items.NETHERITE_SWORD)));
            assertFalse(HistoricalCreativeGrants.rejectsWrite(selection, new ItemStack(Items.NETHERITE_SWORD), ItemStack.EMPTY));
        }
    }
    @Test void beta181DoesNotGainLaterPlantsStoneVariantsOrMenuEntries() {
        for (String item : List.of("stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks", "glass_pane", "melon", "vine")) assertTrue(has("beta_1_8_1", item), item);
        for (String item : List.of("lily_pad", "chiseled_stone_bricks", "fern", "spruce_leaves", "creeper_spawn_egg", "potion", "charcoal", "snow", "emerald")) assertFalse(has("beta_1_8_1", item), item);
    }
    @Test void woodSlabAndStoneRepresentationsHaveTheReplacementBoundary() {
        assertTrue(has("release_1_1", "petrified_oak_slab")); assertFalse(has("release_1_1", "oak_slab"));
        assertTrue(has("release_1_2_5", "birch_planks")); assertFalse(has("release_1_1", "birch_planks"));
        assertFalse(has("release_1_2_5", "birch_stairs")); assertTrue(has("release_1_3", "birch_stairs"));
        assertFalse(has("release_1_3", "petrified_oak_slab")); assertTrue(has("release_1_3", "oak_slab"));
        assertTrue(has("beta_1_8_1", "smooth_stone_slab")); assertFalse(has("release_1_6_4", "stone_slab"));
    }
    @Test void laterInfdevBucketsAndAlphaCowProductsDoNotLeakIntoEarlierRepresentedProfiles() {
        assertFalse(has("infdev_611", "bucket")); assertTrue(has("alpha_1_0_1", "bucket"));
        assertFalse(has("alpha_1_0_6", "milk_bucket")); assertTrue(has("alpha_1_0_11", "milk_bucket"));
        assertFalse(has("indev_latest", "leather")); assertTrue(has("alpha_1_0_11", "leather"));
    }
    @Test void coloredVariantsAreNotExtrapolatedFromTheBaseBlock() {
        assertTrue(has("beta_1_3", "red_bed")); assertFalse(has("release_1_6_4", "blue_bed"));
        assertFalse(has("alpha_1_2_0", "red_wool")); assertTrue(has("beta_1_2", "red_wool"));
        assertFalse(has("release_1_5", "blue_carpet")); assertTrue(has("release_1_6_4", "blue_carpet"));
        assertFalse(has("release_1_6_4", "red_stained_glass"));
        assertFalse(has("release_1_6_4", "spruce_fence")); assertFalse(has("release_1_6_4", "spruce_door"));
    }
    @Test void releaseAdditionsDoNotLeakBackIntoEarlierCatalogs() {
        assertFalse(has("release_1_2_5", "emerald_block")); assertTrue(has("release_1_3", "emerald_block"));
        assertFalse(has("release_1_3", "anvil")); assertTrue(has("release_1_4", "anvil"));
        assertFalse(has("release_1_4", "hopper")); assertTrue(has("release_1_5", "hopper"));
        assertFalse(has("release_1_5", "name_tag")); assertTrue(has("release_1_6_4", "name_tag"));
    }
    @Test void eggItemsAreNotDatedJustByTheEntityExisting() {
        assertFalse(has("release_1_0_0", "creeper_spawn_egg")); assertTrue(has("release_1_1", "creeper_spawn_egg"));
        assertFalse(has("release_1_1", "ocelot_spawn_egg")); assertTrue(has("release_1_2_5", "ocelot_spawn_egg"));
        for (String item : List.of("giant_spawn_egg", "iron_golem_spawn_egg", "wither_skeleton_spawn_egg", "zombie_villager_spawn_egg", "donkey_spawn_egg", "mule_spawn_egg")) assertFalse(has("release_1_6_4", item));
    }
    @Test void snapshotCatalogIsTheStatedParentNotAllModernItems() {
        for (var entry : HistoricalCreativeCatalog.all().keySet()) {
            assertEquals(HistoricalCreativeCatalog.available(P + "release_1_1", entry), HistoricalCreativeCatalog.available(P + "snapshot_11w49a", entry));
            assertEquals(HistoricalCreativeCatalog.available(P + "release_1_4", entry), HistoricalCreativeCatalog.available(P + "snapshot_12w49a", entry));
        }
    }
    @Test void unknownComponentsAndForgedEntityOrContainerDataCannotBecomeNewGrants() {
        var stone = new ItemStack(Items.STONE);
        assertTrue(grants("release_1_1", stone));
        var tag = new CompoundTag(); tag.putString("id", "minecraft:giant");
        var egg = new ItemStack(Items.CREEPER_SPAWN_EGG); egg.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
        assertFalse(grants("release_1_1", egg));
        stone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); assertFalse(grants("release_1_1", stone));
        var chest = new ItemStack(Items.CHEST); chest.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        assertFalse(grants("release_1_1", chest));
    }
    @Test void namedWornOldToolsRemainMovableWithoutRewritingTheirComponents() {
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.set(DataComponents.CUSTOM_NAME, Component.literal("Owner sentinel")); tool.setDamageValue(71); tool.set(DataComponents.REPAIR_COST, 5);
        var before = tool.copy(); assertTrue(grants("release_1_1", tool)); assertTrue(ItemStack.matches(before, tool));
        assertFalse(HistoricalCreativeGrants.rejectsWrite(P + "release_1_1", tool, ItemStack.EMPTY));
    }
    @Test void oversizedUnstackableNewGrantsAreRejectedButIdenticalOwnedNoopsRemainIntact() {
        var tool = new ItemStack(Items.DIAMOND_PICKAXE, 64);
        assertFalse(grants("release_1_1", tool));
        assertFalse(HistoricalCreativeGrants.rejectsWrite(P + "release_1_1", tool, tool.copy()));
        assertTrue(HistoricalCreativeGrants.rejectsWrite(P + "release_1_1", tool, ItemStack.EMPTY));
    }
    @Test void bothSidesOfAnEmptyThenFillMovePreserveUnavailableOwnedStacks() {
        var old = new ItemStack(Items.NETHERITE_SWORD);
        var tag = new CompoundTag(); tag.putString("owner", "sentinel"); old.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); old.setDamageValue(123);
        var before = old.copy();
        assertTrue(HistoricalCreativeGrants.rejectsWrite(P + "release_1_1", old, ItemStack.EMPTY));
        assertTrue(HistoricalCreativeGrants.rejectsWrite(P + "release_1_1", ItemStack.EMPTY, old));
        assertTrue(HistoricalCreativeGrants.rejectsWrite(P + "release_1_1", old, new ItemStack(Items.STONE)));
        assertFalse(HistoricalCreativeGrants.rejectsWrite(P + "release_1_1", old, old.copy()));
        assertTrue(ItemStack.matches(before, old));
    }
    @Test void potionVariantsAndCustomEffectsAreSeparatelyDated() {
        var speed = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
        assertFalse(grants("release_1_2_5", speed)); assertTrue(grants("release_1_3", speed));
        assertFalse(grants("release_1_3", PotionContents.createItemStack(Items.POTION, Potions.HEALING)));
        assertTrue(grants("release_1_4", PotionContents.createItemStack(Items.POTION, Potions.HEALING)));
        assertFalse(grants("release_1_6_4", PotionContents.createItemStack(Items.POTION, Potions.WATER_BREATHING)));
        assertFalse(grants("release_1_6_4", PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER)));
        speed.set(DataComponents.POTION_CONTENTS, new PotionContents(java.util.Optional.of(Potions.SWIFTNESS), java.util.Optional.empty(), List.of(new MobEffectInstance(MobEffects.DARKNESS, 200))));
        assertFalse(grants("release_1_6_4", speed));
    }
    @Test void booksCannotSmuggleLaterEnchantmentsOrOverlevels() {
        var holder = registries.lookupOrThrow(Registries.ENCHANTMENT);
        var book = new ItemStack(Items.ENCHANTED_BOOK); var values = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        values.set(holder.getOrThrow(Enchantments.UNBREAKING), 3); book.set(DataComponents.STORED_ENCHANTMENTS, values.toImmutable());
        assertTrue(grants("snapshot_12w49a", book)); assertFalse(grants("release_1_3", book));
        values.set(holder.getOrThrow(Enchantments.MENDING), 1); book.set(DataComponents.STORED_ENCHANTMENTS, values.toImmutable());
        assertFalse(grants("snapshot_12w49a", book));
        values = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY); values.set(holder.getOrThrow(Enchantments.UNBREAKING), 4);
        book.set(DataComponents.STORED_ENCHANTMENTS, values.toImmutable()); assertFalse(grants("snapshot_12w49a", book));
        values = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY); values.set(holder.getOrThrow(Enchantments.THORNS), 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, values.toImmutable()); assertFalse(grants("snapshot_12w49a", book));
    }
    @Test void generatedLegacyCatalogUsesRealVariantsWithoutGlobalCacheMutation() {
        var old = HistoricalCreativeInventory.entries(P + "snapshot_11w49a", registries);
        assertTrue(old.size() > 150); assertTrue(old.stream().allMatch(stack -> grants("snapshot_11w49a", stack)));
        assertTrue(old.stream().anyMatch(stack -> stack.is(Items.PETRIFIED_OAK_SLAB)));
        assertFalse(old.stream().anyMatch(stack -> stack.is(Items.OAK_SLAB) || stack.is(Items.NETHERITE_PICKAXE)));
    }
}
