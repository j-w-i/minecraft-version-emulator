/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.mixin.common;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes ores drop themselves, in versions that had no raw metals.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mechanics_block/BlockBehaviourMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.SELF_BLOCK_DROPS}. The injection point
 * - {@code @ModifyReturnValue} on {@code getDrops} at the second RETURN - and the replacement itself are upstream's:
 * the drops list is cleared and the block's own item is added.
 *
 * <h2>Why both RETURNs, and the bug that taught me</h2>
 *
 * <p>{@code getDrops} has two returns: the first fires when the block has <b>no loot table at all</b> and hands
 * back {@code Collections.emptyList()}, and the second fires after a loot table was rolled. The upstream port used
 * the second, which is the one an ore takes - and that was correct for ores and silently wrong for everything
 * without a loot table.
 *
 * <p>The cost of getting that wrong was a shipped defect the owner found by playing: Beta 1.9 Pre3's End portal
 * frames blew up (the explosion-resistance hook worked, and is a different mixin) and dropped <b>nothing</b>,
 * because {@code end_portal_frame} has no loot table in 1.21.1 - verified by listing the jar, where
 * {@code data/minecraft/loot_table/blocks/end_portal_frame.json} does not exist while its blockstate and models do
 * - so it takes the early return and the ordinal-1 injection never ran.
 *
 * <p>The pair below is therefore not redundancy. Each handler names the path it serves: the ordinal-0 one for
 * blocks with no loot table, the ordinal-1 one for rolled tables. The frame rule is claimed by the first, the ore
 * rule by the second, and the spawner rule by both - because "no loot table" is an implementation detail of which
 * blocks happen to have one, and a rule keyed on a block rather than on that detail must not care which path the
 * engine chose. Both handlers count RETURNs of the same type, so ordinal 1 is still the post-roll return; a third
 * return added by a future version would need this looked at again rather than assumed.
 *
 * <h2>Scope</h2>
 *
 * <p>Upstream's listing holds iron, gold and copper ore plus their deepslate variants. Four of the six cannot exist
 * in any declared profile - the newest is Release 1.6.4 - and they are kept so the port matches upstream. The
 * boundary is Release 1.17, where raw metals arrive, which is later than every profile; this project's own
 * availability data already dates raw_iron, raw_gold and raw_copper to 1.17.
 *
 * <p>A shared mixin: block drops are rolled server-side.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourHistoricalDropMixin {
    /**
     * The early return: this block has no loot table, so vanilla hands back an empty list.
     *
     * <p>Upstream did not have this handler. It exists because the End portal frame is one of these blocks and the
     * upstream-shaped injection could not reach it.
     */
    @ModifyReturnValue(method = "getDrops", at = @At(value = "RETURN", ordinal = 0))
    private List<ItemStack> historicalExperience$selfDropsWithoutLootTable(List<ItemStack> drops,
            BlockState blockState) {
        if (!Features.active(Feature.ORE_DROPS_AS_BLOCKS)) {
            return drops;
        }

        // Beta 1.9 Pre3 ONLY: the frame dropped as an item when an explosion destroyed it, which Pre4 fixed.
        // minecraft.wiki, End Portal Frame history: Pre3 "It can drop as an item due to explosions", Pre4 "The
        // End portal can no longer be broken by explosions". Mined directly it stays unobtainable - its hardness
        // is -1 - so an explosion is the only way to reach this, and the frame has NO loot table, which is exactly
        // why this handler and not the other one is the one that must carry it.
        //
        // Returns a NEW list rather than clearing the one it was given, and that is the second half of the owner's
        // bug rather than a style preference. This return path hands back `Collections.emptyList()`, which is
        // IMMUTABLE, so `drops.clear()` threw UnsupportedOperationException inside the explosion's own drop call -
        // the engine swallowed it and the frame dropped nothing. The runtime probe caught it as
        // `endPortalFrame: probeFailed: java.lang.UnsupportedOperationException` on Pre3 alone, which is also the
        // proof that the explosion really does route through this method. A mutable list is the fix; the two
        // mutation sites below were changed for the same reason, since a loot roll is not guaranteed mutable either.
        if (blockState.is(Blocks.END_PORTAL_FRAME) && Features.active(Feature.END_PORTAL_FRAME_EXPLODES)) {
            return List.of(blockState.getBlock().asItem().getDefaultInstance());
        }
        return drops;
    }

    /** Upstream: {@code nt_mechanics_block$modifyBlockDrops}. The post-roll return, which an ore takes. */
    @ModifyReturnValue(method = "getDrops", at = @At(value = "RETURN", ordinal = 1))
    private List<ItemStack> historicalExperience$selfDrops(List<ItemStack> drops, BlockState blockState,
            LootParams.Builder params) {
        if (!Features.active(Feature.ORE_DROPS_AS_BLOCKS)) {
            return drops;
        }

        // Beta 1.9 Pre4 ONLY: Silk Touch made monster spawners drop themselves; Pre5 removed it, and the modern game
        // has never had it. THIS RULE NEEDS THE TOOL, and it belongs on THIS return rather than the other one.
        //
        // Two wrong shapes were tried before this one, and both are worth naming because they are the same mistake
        // in different clothes - assuming how the engine reaches a rule instead of checking.
        //
        //  * It first looked for a spawner item in the ROLLED LIST, reasoning that only Silk Touch produces one. A
        //    modern monster spawner drops NOTHING by design: its loot table is literally
        //    `{"type":"minecraft:block","random_sequence":"minecraft:blocks/spawner"}` with no pools, and its only
        //    reward is experience, which arrived in 12w22a. So the roll is ALWAYS empty and the rule could never
        //    fire - which is what the owner saw when mining a spawner in a Pre4 world with a Silk Touch pickaxe.
        //  * It then went on the ordinal-0 "no loot table" handler, where the End portal frame rule lives. But the
        //    spawner DOES have a loot table file, empty though it is, so it takes this return and not that one.
        //
        // `Block.getDrops` sets LootContextParams.TOOL with withParameter and LootParams$Builder.getOptionalParameter
        // reads it back, both verified by disassembling the mapped jar, so the Silk Touch test is a real one now.
        // The expensive registry lookup happens only for a spawner and only in the one build that drops it.
        if (blockState.is(Blocks.SPAWNER)) {
            if (Features.active(Feature.SILKY_SPAWNERS) && hasSilkTouch(params)) {
                return List.of(blockState.getBlock().asItem().getDefaultInstance());
            }
            // Not the build, or not the tool: a spawner yields nothing, which is what it still does today. Returning
            // explicitly rather than dropping through matters, because the ore rule below must not see a spawner.
            return List.of();
        }

        // The End portal frame rule is deliberately NOT here. It lives in the ordinal-0 handler above, because the
        // frame has no loot table and therefore never reaches this return. Keeping a copy here was the first shape
        // of the fix and it was wrong in a way worth naming: dead code that reads like the live rule, so a later
        // reader would find two frame rules and no way to tell which one the game takes.

        // Alpha 1.0.6 ONLY: breaking a snow block returned SIX snowballs while the recipe only cost four, so every
        // craft-and-break cycle produced two free snowballs. The recipe half is already modern - the jar's
        // data/minecraft/recipe/snow_block.json is a 2x2 of snowballs and its loot table sets a count of four - so
        // the historical behaviour is exactly this one build returning six. The count is written here rather than in
        // the gate because the wiki quote states the number and the gate stays a yes/no question.
        if (blockState.is(Blocks.SNOW_BLOCK) && Features.active(Feature.SNOWBALL_SURPLUS)) {
            return List.of(new ItemStack(Items.SNOWBALL, 6));
        }

        // The spawner rule was here in an earlier shape and has moved above, where it can read the tool. It is NOT
        // restored here, because the stale version of it reasoned that "the rolled list is non-empty only when Silk
        // Touch applied" - true for a block whose loot table has pools, and false for a spawner, which drops nothing
        // in this version and therefore rolled an empty list every time. Left as a note so the wrong shape is not
        // reintroduced.

        if (isOre(blockState)) {
            return List.of(blockState.getBlock().asItem().getDefaultInstance());
        }

        return drops;
    }

    /**
     * Whether the loot parameters say the breaking tool was enchanted with Silk Touch.
     *
     * <h2>Why the tool has to come from the loot parameters</h2>
     *
     * <p>{@code BlockBehaviour.getDrops(BlockState, LootParams$Builder)} has no tool argument. The tool reaches this
     * method inside the builder, because {@code Block.getDrops} sets {@link LootContextParams#TOOL} with
     * {@code withParameter} - disassembled from the mapped jar, not assumed - so
     * {@code getOptionalParameter} reads it back. That makes the check exact rather than a guess, and the guess is
     * what broke this rule the first time.
     *
     * <h2>Kept private, and why that is load-bearing</h2>
     *
     * <p>This method must stay {@code private}. It was made {@code public} for one round so a test could call it, and
     * that <b>crashed every dedicated-server launch</b>: {@code InvalidMixinException: Mixin ... contains non-private
     * static method isSpawnerDrop(...)}. Mixin refuses to transform a mixin class whose static helpers are visible,
     * because they would be merged onto the target and become part of {@code BlockBehaviour}'s API. A green compile
     * says nothing about that - only starting the game does.
     *
     * <p>Being private is also why the version half is what carries the unit test
     * ({@code appliesTo("silkySpawners", ...)} is true for exactly Pre4 of the 32 declared builds) and the runtime
     * block-drop probe carries the rest: it calls the public {@code getDrops} entry point that a player's pickaxe
     * and an explosion both reach.
     */
    private static boolean hasSilkTouch(LootParams.Builder params) {
        if (params == null) {
            return false;
        }
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null || tool.isEmpty()) {
            return false;
        }
        var server = params.getLevel().getServer();
        if (server == null) {
            return false;
        }
        try {
            var enchantments = server.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var silkTouch = enchantments.getHolderOrThrow(
                net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            return net.minecraft.world.item.enchantment.EnchantmentHelper
                .getItemEnchantmentLevel(silkTouch, tool) > 0;
        } catch (RuntimeException missingRegistry) {
            // A tool cannot be tested against an enchantment registry that is not loaded. Answering false leaves the
            // spawner dropping nothing, which is the modern behaviour and the safe direction - a wrong true would
            // hand out a block the version never dropped.
            return false;
        }
    }

    /** Upstream's listing, verbatim. */
    private static boolean isOre(BlockState blockState) {
        return blockState.is(Blocks.IRON_ORE)
            || blockState.is(Blocks.GOLD_ORE)
            || blockState.is(Blocks.COPPER_ORE)
            || blockState.is(Blocks.DEEPSLATE_IRON_ORE)
            || blockState.is(Blocks.DEEPSLATE_GOLD_ORE)
            || blockState.is(Blocks.DEEPSLATE_COPPER_ORE);
    }
}
