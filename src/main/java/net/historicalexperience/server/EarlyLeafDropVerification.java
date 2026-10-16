/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.server;

import java.util.LinkedHashMap;
import java.util.Map;
import net.historicalexperience.gameplay.EarlyGameplayRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Rolls actual mixed-in loot tables/public block drop API; no placed blocks, entities, account data or world RNG reseeding. */
final class EarlyLeafDropVerification {
    private EarlyLeafDropVerification() { }
    static Map<String, Object> verify(ServerLevel level, String profile) {
        if (!Boolean.getBoolean("historical_experience.generationProbe")) throw new IllegalStateException("Probe only");
        var state = Blocks.OAK_LEAVES.defaultBlockState();
        var params = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, state)
            .withParameter(LootContextParams.ORIGIN, Vec3.ZERO).withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
            .create(LootContextParamSets.BLOCK);
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.withDefaultNamespace("blocks/oak_leaves")));
        var observed = new java.util.TreeMap<String, Integer>();
        var random = RandomSource.create(0x4845584c454146L);
        for (int i = 0; i < 10000; i++) for (var item : table.getRandomItems(params, random)) {
            observed.merge(BuiltInRegistries.ITEM.getKey(item.getItem()).toString(), item.getCount(), Integer::sum);
        }
        boolean early = EarlyGameplayRecipes.supports(profile);
        if (early) {
            if (observed.size() != 1 || !observed.containsKey("minecraft:oak_sapling")) {
                throw new IllegalStateException("Early oak leaf loot leaked modern items: " + observed);
            }
            int expected = 0; var reference = RandomSource.create(0x4845584c454146L);
            for (int i = 0; i < 10000; i++) if (reference.nextInt(10) == 0) expected++;
            if (observed.get("minecraft:oak_sapling") != expected) throw new IllegalStateException("Leaf quantity/random consumption differs: " + observed + " expected " + expected);
            verifyBucketsAndTools(level, table);
            for (int i = 0; i < 256; i++) for (var item : Block.getDrops(state, level, BlockPos.ZERO, null)) {
                if (!item.is(net.minecraft.world.item.Items.OAK_SAPLING) || item.getCount() != 1) {
                    throw new IllegalStateException("Public block drop API bypassed early leaf rule: " + item);
                }
            }
        } else if (profile == null && (observed.getOrDefault("minecraft:apple", 0) == 0
                || observed.getOrDefault("minecraft:stick", 0) == 0 || observed.getOrDefault("minecraft:oak_sapling", 0) == 0)) {
            throw new IllegalStateException("Ordinary oak leaves lost modern drops: " + observed);
        }
        var report = new LinkedHashMap<String, Object>();
        report.put("seededTableRolls", 10000); report.put("ordinary", !early); report.put("items", observed);
        report.put("publicBlockDropCalls", early ? 256 : 0);
        report.put("exactBucketToolStateCases", early ? 80 : 0);
        report.put("explosionCompatibilityCases", early ? 3 : 0);
        return report;
    }
    private static void verifyBucketsAndTools(ServerLevel level, net.minecraft.world.level.storage.loot.LootTable table) {
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var silk = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        silk.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        var fortune = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        fortune.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
        for (var tool : java.util.List.of(ItemStack.EMPTY, new ItemStack(net.minecraft.world.item.Items.SHEARS), silk, fortune)) {
            var before = tool.copy();
            for (boolean persistent : new boolean[] {false, true}) for (int roll = 0; roll < 10; roll++) {
                var state = Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, persistent)
                    .setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, persistent ? 1 : 7);
                var params = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, state)
                    .withParameter(LootContextParams.TOOL, tool).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.BLOCK);
                var exact = new ExactRandom(roll, false, 0);
                var output = table.getRandomItems(params, exact);
                assertOutput(output, roll == 0);
                if (exact.ints != 1 || exact.floats != 0 || !ItemStack.matches(tool, before)) {
                    throw new IllegalStateException("Leaf tool/state probe changed components or consumed extra random draws");
                }
            }
        }
        // Modern explosion predicate compatibility ONLY, not a historical blast-rate assertion.
        for (int variant = 0; variant < 3; variant++) {
            var params = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, Blocks.OAK_LEAVES.defaultBlockState())
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.EXPLOSION_RADIUS, 2.0F).create(LootContextParamSets.BLOCK);
            var exact = new ExactRandom(variant == 2 ? 1 : 0, true, variant == 1 ? 0.99F : 0.0F);
            var context = new net.minecraft.world.level.storage.loot.LootContext.Builder(params)
                .withOptionalRandomSource(exact).create(java.util.Optional.empty());
            var output = new java.util.ArrayList<ItemStack>(); table.getRandomItemsRaw(context, output::add);
            assertOutput(output, variant == 0);
            if (exact.ints != 1 || exact.floats != (variant == 2 ? 0 : 1)) throw new IllegalStateException("Explosion compatibility draw contract changed");
        }
    }
    private static void assertOutput(java.util.List<ItemStack> items, boolean expected) {
        if (items.size() != (expected ? 1 : 0) || (expected && (!items.getFirst().is(net.minecraft.world.item.Items.OAK_SAPLING)
                || items.getFirst().getCount() != 1))) throw new IllegalStateException("Wrong exact leaf bucket output: " + items);
    }
    private static final class ExactRandom extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        final int roll; final boolean blast; final float fraction; int ints, floats;
        ExactRandom(int roll, boolean blast, float fraction) { super(0); this.roll = roll; this.blast = blast; this.fraction = fraction; }
        @Override public int nextInt(int bound) {
            if (bound != 10) throw new IllegalStateException("Unexpected leaf bound " + bound);
            ints++; return roll;
        }
        @Override public float nextFloat() {
            if (!blast) throw new IllegalStateException("Modern leaf probability/function leaked into source bucket test");
            floats++; return fraction;
        }
        @Override public int next(int bits) { throw new IllegalStateException("Unexpected random primitive in leaf test: " + bits); }
    }
}
