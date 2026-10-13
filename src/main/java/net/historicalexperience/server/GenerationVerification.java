package net.historicalexperience.server;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.historicalexperience.HistoricalExperienceMod;
import net.historicalexperience.generation.BetaOreRules;
import net.historicalexperience.generation.HistoricalFeatureExclusions;
import net.historicalexperience.generation.HistoricalStructureRules;
import net.historicalexperience.gameplay.HistoricalSpawnRules;
import net.historicalexperience.integration.modernerbeta.ModernerBetaAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Opt-in test runner only. Never registered in an ordinary game launch. */
public final class GenerationVerification {
    private GenerationVerification() {}

    public static void registerIfRequested() {
        if (Boolean.getBoolean("historical_experience.generationProbe")) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> server.execute(() -> run(server)));
        }
    }

    /**
     * Observes the simulation rules in a live world, which the generation checks cannot see.
     *
     * <p>The loot tables, the spawn-gear gate and the baby-zombie gate are all unit-tested, but until this
     * existed none of them had been watched happening. That is the gap this closes, and it is the same gap
     * that let the drop tables sit as dead code: a rule can be correct in a test and never applied in a
     * world, and the two failures look identical from a unit test's point of view.
     *
     * <p>A zombie is the subject because every one of these rules has an opinion about it: it has a table
     * entry in both researched versions, it must not spawn geared before Release 1.4.2, it must not be a
     * baby before Release 1.6.2, and a version with no experience must leave no orbs.
     */
    private static Map<String, Object> gameplayProbe(net.minecraft.server.level.ServerLevel level,
            String versionId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("versionId", versionId);
        java.util.List<String> drops = new java.util.ArrayList<>();
        int orbs = 0;
        boolean baby = false;
        boolean geared = false;
        try {
            var pos = new BlockPos(16, 90, 16);
            var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.spawn(level, pos,
                net.minecraft.world.entity.MobSpawnType.COMMAND);
            if (zombie == null) {
                out.put("failure", "the zombie could not be spawned; the chunk may not be loaded");
            } else {
                // Read the spawn result BEFORE killing it: these are the same two gates the mixins apply.
                baby = zombie.isBaby();
                geared = !zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).isEmpty()
                    || !zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty();
                // Asked BEFORE the kill, so the answer explains the orb count instead of being guessed at.
                // A version with no experience should leave none; if this is false, then zero orbs prove
                // nothing about the suppression hook, and saying so is the point of recording it.
                out.put("shouldDropExperience", zombie.shouldDropExperience());
                zombie.hurt(level.damageSources().generic(), 1000.0F);
                if (zombie.isAlive()) {
                    zombie.kill();
                }
                var box = new net.minecraft.world.phys.AABB(6, 40, 6, 26, 127, 26);
                for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        box)) {
                    drops.add(net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(item.getItem().getItem()).toString()
                        + " x" + item.getItem().getCount());
                }
                orbs = level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, box).size();
            }
        } catch (Throwable failure) {
            out.put("failure", failure.toString());
        }
        java.util.Collections.sort(drops);
        out.put("zombieDrops", drops);
        out.put("experienceOrbs", orbs);
        out.put("babyZombie", baby);
        out.put("gearedZombie", geared);
        return out;
    }

    /**
     * What the era's damage table does to a few real items, next to what the modern component says.
     *
     * <p>Written because "the damage values did not change" was reported for items where no change is
     * expected: 1.9's 15w34c reduced swords by one and raised axes by four then three, so a diagonal sword is
     * the same number on both sides of the boundary. A probe that prints both numbers turns that into a fact
     * rather than an argument.
     */
    private static Map<String, Object> damageTableProbe() {
        Map<String, Object> out = new LinkedHashMap<>();
        java.util.List<net.minecraft.world.item.Item> items = java.util.List.of(
            net.minecraft.world.item.Items.WOODEN_SWORD, net.minecraft.world.item.Items.DIAMOND_SWORD,
            net.minecraft.world.item.Items.DIAMOND_AXE, net.minecraft.world.item.Items.IRON_AXE,
            net.minecraft.world.item.Items.DIAMOND_PICKAXE, net.minecraft.world.item.Items.DIAMOND_SHOVEL,
            net.minecraft.world.item.Items.DIAMOND_HOE);
        for (net.minecraft.world.item.Item item : items) {
            var stack = new net.minecraft.world.item.ItemStack(item);
            var modern = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
            if (modern == null) {
                out.put(item.toString(), "no attribute component");
                continue;
            }
            double modernDamage = modern.compute(1.0,
                net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            double eraDamage = net.historicalexperience.gameplay.HistoricalDamageValues
                .rewrite(stack, modern)
                .compute(1.0, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            out.put(item.toString(), "applicable="
                + net.historicalexperience.gameplay.HistoricalDamageValues.isApplicable(stack)
                + " modern=" + modernDamage + " era=" + eraDamage
                + (modernDamage == eraDamage ? " (SAME on both sides of the boundary)" : ""));
        }
        return out;
    }

    /**
     * Loads the classes my mixins target, because loading a class is what transforms it.
     *
     * <p>A failed injection is fatal at transformation, and transformation happens at class load. A dedicated
     * server never loads {@code FoodData} - nothing places a player - so an injection that broke every join was
     * invisible to every boot check this project ran. Instantiating the class here surfaces that failure in a
     * server run instead, which is what the probe is for.
     *
     * <p>Client-only targets (Gui, HumanoidArmorLayer, ItemInHandRenderer, ParticleEngine, ClientLevel,
     * Minecraft, HumanoidModel, AbstractZombieModel, AnimationUtils, SkeletonModel, GuiAccess, SheepAccess) cannot
     * be loaded on a server and are listed in the report as NOT checked, rather than left to look covered.
     */
    private static Map<String, Object> mixinTargetSmokeTest() {
        Map<String, Object> out = new LinkedHashMap<>();
        java.util.List<String> serverTargets = java.util.List.of(
            "net.minecraft.world.food.FoodData",
            "net.minecraft.world.level.block.state.BlockBehaviour",
            "net.minecraft.world.item.component.ItemAttributeModifiers",
            "net.minecraft.world.entity.LivingEntity",
            "net.minecraft.world.entity.Mob",
            "net.minecraft.world.entity.player.Player",
            "net.minecraft.world.entity.ExperienceOrb",
            "net.minecraft.server.level.ServerLevel",
            // The prerelease gates. Each of these carries an injection whose target has to resolve, and a wrong
            // target is fatal when the class is transformed - which is a player opening a table, using an eye of
            // ender, or letting a stand finish brewing. Loading them here turns that into a probe failure.
            "net.minecraft.world.inventory.EnchantmentMenu",
            "net.minecraft.world.item.EnderEyeItem",
            "net.minecraft.world.level.block.entity.BrewingStandBlockEntity",
            "net.minecraft.world.entity.Entity");

        for (String name : serverTargets) {
            try {
                Class<?> type = Class.forName(name);
                out.put(name, "loaded and transformed");
                if (name.endsWith("FoodData")) {
                    // Instantiating is the strongest form of this check available without a player: it runs the
                    // constructor as well as the transformation.
                    type.getDeclaredConstructor().newInstance();
                    out.put(name, "loaded, transformed and constructed");
                }
            } catch (Throwable failure) {
                out.put(name, "FAILED: " + failure);
            }
        }

        out.put("client-only targets NOT checked by a server run",
            java.util.List.of("Gui", "HumanoidArmorLayer", "ItemInHandRenderer", "ParticleEngine", "ClientLevel",
                "Minecraft", "HumanoidModel", "AbstractZombieModel", "AnimationUtils", "SkeletonModel"));
        return out;
    }

    /**
     * Breaks ore blocks and reports what actually dropped, through the real mining path.
     *
     * <p>Written because the ore self-drop rule was reported WORKING and is disputed. The rule replaces the drops
     * of iron and gold ore with the ore block itself, and in 1.21.1 those drops come from datapack loot tables,
     * which is a good reason to doubt that a hook on the block's own drop method is reached. Reading the code
     * cannot settle it and neither can reading the loot table: only breaking a block can.
     *
     * <p>So this places each ore, calls {@code destroyBlock} with drops enabled - the same path a player's pickaxe
     * takes - and reads the item entities that resulted. It reports what appeared, not what was expected.
     */
    private static Map<String, Object> oreDropProbe(net.minecraft.server.level.ServerLevel level) {
        Map<String, Object> out = new LinkedHashMap<>();

        for (String oreId : java.util.List.of("minecraft:iron_ore", "minecraft:gold_ore")) {
            try {
                var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .get(net.minecraft.resources.ResourceLocation.parse(oreId));
                if (block == net.minecraft.world.level.block.Blocks.AIR) {
                    out.put(oreId, "<no such block in this version's registry>");
                    continue;
                }

                var pos = new BlockPos(20, 90, 20);
                level.setBlockAndUpdate(pos, block.defaultBlockState());
                // true = let the drops be produced and spawned, which is what mining does.
                level.destroyBlock(pos, true);

                java.util.List<String> dropped = new java.util.ArrayList<>();
                for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(3.0))) {
                    dropped.add(net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(item.getItem().getItem()) + " x" + item.getItem().getCount());
                    item.discard();
                }

                out.put(oreId, dropped.isEmpty() ? "<nothing dropped>" : dropped);
            } catch (Throwable failure) {
                out.put(oreId, "probe failed: " + failure);
            }
        }

        return out;
    }

    private static void run(MinecraftServer server) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("runToken", System.getProperty("historical_experience.generationProbe.token"));
        report.put("historicalState", HistoricalUpgradeService.diagnostics(server.getWorldPath(LevelResource.ROOT)));
        try {
            var level = server.overworld();
            var generator = level.getChunkSource().getGenerator();
            String alias = HistoricalFeatureExclusions.historicalAlias(generator);
            report.put("alias", alias);
            report.put("generator", generator.getClass().getName());
            report.put("levelMinY", level.getMinBuildHeight());
            report.put("levelHeight", level.getHeight());
            report.put("generatorMinY", generator.getMinY());
            report.put("generatorDepth", generator.getGenDepth());
            report.put("seaLevel", generator.getSeaLevel());
            if (alias != null) {
                report.put("appliedSettings", ModernerBetaAdapter.compareToUpstreamBeta173(generator));
                report.put("appliedDeltas", ModernerBetaAdapter.describeAppliedRecipeDeltas(generator));
            }
            int radius = Integer.getInteger("historical_experience.generationProbe.radius", 3);
            int center = Integer.getInteger("historical_experience.generationProbe.center", 0);
            if (radius < 0 || radius > 8) throw new IllegalArgumentException("radius must be 0..8");
            Map<String, Integer> biomes = new TreeMap<>();
            int sampled = 0;
            Map<String, Long> sampledBlocks = new TreeMap<>();
            for (int x = center - radius; x <= center + radius; x++) {
                for (int z = center - radius; z <= center + radius; z++) {
                    var chunk = level.getChunk(x, z);
                    for (var section : chunk.getSections()) {
                        section.getStates().count((state, count) -> sampledBlocks.merge(
                            net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                .getKey(state.getBlock()).toString(), (long) count, Long::sum));
                    }
                    sampled++;
                    for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y += 8) {
                        var biome = level.getBiome(new BlockPos(x * 16 + 8, y, z * 16 + 8));
                        String id = biome.unwrapKey().orElseThrow().location().toString();
                        biomes.merge(id, 1, Integer::sum);
                    }
                }
            }
            // Exercise the actual injected decorator funnel without placing or removing any blocks.
            java.util.List<String> decoratorWrites = new java.util.ArrayList<>();
            var decorator = new net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator.Context(
                level, (pos, state) -> decoratorWrites.add(state.toString()),
                net.minecraft.util.RandomSource.create(1L), java.util.Set.of(),
                java.util.Set.of(), java.util.Set.of());
            decorator.setBlock(BlockPos.ZERO, net.minecraft.world.level.block.Blocks.BEE_NEST.defaultBlockState());
            decorator.setBlock(BlockPos.ZERO, net.minecraft.world.level.block.Blocks.VINE.defaultBlockState());
            decorator.setBlock(BlockPos.ZERO, net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());
            report.put("decoratorControlWrites", decoratorWrites);
            report.put("sampledFullChunks", sampled);
            report.put("biomes", biomes);
            report.put("sampledFullBlockCounts", sampledBlocks);
            Map<String, Long> paletteFailures = new TreeMap<>();
            Map<String, Long> unknownBlocks = new TreeMap<>();
            Map<String, String> paletteFailuresEvidence = new TreeMap<>();
            // Judged for EVERY historical profile, not only the Beta 1.7.3 revision that happened to have an
            // extracted palette first. The earlier version of this check was hard-coded to one alias, which
            // is exactly why a real Alpha world could contain ten vines without the probe saying a word.
            String versionId = alias == null ? null
                : net.historicalexperience.version.HistoricalVersions.byRevisionAlias(alias)
                    .map(net.historicalexperience.version.HistoricalVersion::id)
                    .orElse(null);
            report.put("judgedVersionId", versionId);
            // Stand in for the player join that normally sets this. Every session-gated rule - the era's
            // damage values, the 1.9 cooldown and sweep - is inert without it, so a probe that does not set
            // it measures the modern game and reports success.
            if (versionId != null) {
                net.historicalexperience.gameplay.HistoricalActiveSession.set(versionId);
            }
            if (versionId != null) {
                sampledBlocks.forEach((id, count) -> {
                    if (java.util.Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air")
                            .contains(id)) {
                        return;
                    }
                    var verdict = net.historicalexperience.generation.HistoricalBlockContent.judge(versionId, id);
                    if (verdict == net.historicalexperience.generation.HistoricalBlockAvailability.Verdict
                            .ANACHRONISTIC) {
                        paletteFailures.put(id, count);
                        String release = net.historicalexperience.generation.HistoricalBlockContent
                            .introducedInRelease(id);
                        var boundary = net.historicalexperience.generation.HistoricalBlockAvailability
                            .boundaryFor(id);
                        paletteFailuresEvidence.put(id, boundary != null
                            ? "earliest declared version that had it: " + boundary.versionId()
                                + " - " + boundary.evidence()
                            : "introduced in Release " + release + ", later than every declared version");
                    }
                    if (verdict == net.historicalexperience.generation.HistoricalBlockAvailability.Verdict
                            .UNKNOWN) {
                        unknownBlocks.put(id, count);
                    }
                });
            }
            report.put("sampledPaletteFailures", paletteFailures);
            report.put("sampledPaletteFailuresEvidence", paletteFailuresEvidence);
            report.put("sampledUnknownBlocks", unknownBlocks);
            report.put("blockAvailabilityDatedBlocks",
                net.historicalexperience.generation.HistoricalBlockAvailability.size());
            report.put("features", HistoricalFeatureExclusions.diagnostics());
            report.put("suppressedDecorationBlocks", HistoricalFeatureExclusions.suppressedBlockCount());
            report.put("suppressedDecorationIds", HistoricalFeatureExclusions.suppressedBlockIds());
            report.put("ores", BetaOreRules.diagnostics());
            report.put("structureSuppressions", HistoricalStructureRules.suppressedCount());
            report.put("suppressedStructureIds", HistoricalStructureRules.suppressedStructureIds());
            report.put("declaredStructureRuleVersions", HistoricalStructureRules.declaredVersions().toString());
            report.put("allowedStructuresForThisProfile", versionId == null ? "<ordinary world>"
                : HistoricalStructureRules.allowedForVersion(versionId).toString());
            report.put("spawns", HistoricalSpawnRules.diagnostics());
            // The mob-drop replacement and experience suppression, so a real session shows them working
            // instead of only a unit test asserting the tables are readable.
            // The live observation of the simulation rules, which the block and structure checks cannot see.
            report.put("gameplay", gameplayProbe(level, versionId));
            report.put("oreDrops", oreDropProbe(level));
            report.put("damageTable", damageTableProbe());
            report.put("combatHooks", net.historicalexperience.gameplay.HistoricalCombatDiagnostics.report());
            report.put("mixinTargets", mixinTargetSmokeTest());
            // AFTER the gameplay probe, deliberately: the probe kills a zombie, and the counters below are
            // what prove the kill went through the era's drop table. Reading them first reported zero
            // replaced deaths beside two dropped feathers, which is the kind of self-contradicting evidence
            // this project keeps having to unpick.
            report.put("loot", net.historicalexperience.gameplay.HistoricalLootRules.diagnostics());
            String upgradeTo = System.getProperty("historical_experience.generationProbe.upgradeTo");
            if (upgradeTo != null) {
                // Take a flushed checkpoint before the operator path creates its verified backup.
                server.saveEverything(true, true, true);
                var before = HistoricalUpgradeService.diagnostics(server.getWorldPath(LevelResource.ROOT));
                int commandResult = server.getCommands().getDispatcher().execute(
                    "hex upgrade " + upgradeTo, server.createCommandSourceStack());
                report.put("upgradeCommandResult", commandResult);
                report.put("upgradeBefore", before);
                report.put("upgradeStaged", HistoricalUpgradeService.diagnostics(server.getWorldPath(LevelResource.ROOT)));
                report.put("loadedAliasAfterStaging", HistoricalFeatureExclusions.historicalAlias(
                    level.getChunkSource().getGenerator()));
                if (commandResult != 1) throw new IllegalStateException("Upgrade command refused " + upgradeTo);
            }
            report.put("success", true);
        } catch (Throwable failure) {
            report.put("success", false);
            report.put("failure", failure.toString());
            HistoricalExperienceMod.LOGGER.error("Generation verification failed", failure);
        } finally {
            try {
                Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("generation-probe.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report));
            } catch (Exception failure) {
                HistoricalExperienceMod.LOGGER.error("Cannot retain generation verification evidence", failure);
            }
            HistoricalExperienceMod.LOGGER.info("GENERATION_VERIFICATION {}", report);
            server.halt(false);
        }
    }
}
