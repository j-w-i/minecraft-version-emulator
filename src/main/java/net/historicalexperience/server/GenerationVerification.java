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
import net.historicalexperience.gameplay.HistoricalToolDurability;
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
    /**
     * What real tool stacks report for durability, which is the only thing that settles whether the override
     * reaches the component the game actually destroys tools through.
     *
     * <p>Same reasoning as the damage table beside it: reading the code cannot tell whether a
     * {@code DataComponentHolder.getOrDefault} hook is on the path that {@code ItemStack.getMaxDamage} takes, and
     * a unit test on the table proves only that the table is readable. So this builds actual stacks and asks them.
     */
    private static Map<String, Object> toolDurabilityProbe(String versionId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("versionId", versionId);
        Map<String, Integer> values = new java.util.TreeMap<>();
        for (String name : java.util.List.of("wooden_pickaxe", "stone_pickaxe", "iron_pickaxe", "iron_sword",
                "diamond_shovel", "golden_pickaxe", "shears", "flint_and_steel")) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .get(net.minecraft.resources.ResourceLocation.parse("minecraft:" + name));
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                values.put(name, -1);
                continue;
            }
            try {
                values.put(name, new net.minecraft.world.item.ItemStack(item).getMaxDamage());
            } catch (Throwable failure) {
                values.put(name, -2);
            }
        }
        out.put("maxDamageByItem", values);
        out.put("tableSays", HistoricalToolDurability.namedItems().stream().collect(
            java.util.stream.Collectors.toMap(id -> id, id -> String.valueOf(
                HistoricalToolDurability.durabilityFor(versionId, id)), (a, b) -> a, java.util.TreeMap::new)));
        return out;
    }

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
            "net.minecraft.world.item.ItemStack",
            "net.minecraft.world.level.portal.PortalShape",
            "net.minecraft.world.level.block.NetherPortalBlock",
            // The prerelease gates. Each of these carries an injection whose target has to resolve, and a wrong
            // target is fatal when the class is transformed - which is a player opening a table, using an eye of
            // ender, or letting a stand finish brewing. Loading them here turns that into a probe failure.
            "net.minecraft.world.inventory.EnchantmentMenu",
            "net.minecraft.world.item.EnderEyeItem",
            "net.minecraft.world.level.block.entity.BrewingStandBlockEntity",
            "net.minecraft.world.entity.Entity",
            // The food-stacking rule, after the owner reported that pickup and a double-click both still made a
            // stack of food before Beta 1.8. Each of these three carries a redirect whose target must resolve,
            // and a wrong target is fatal when the class is transformed - that is, when a player opens an
            // inventory. Container is an interface whose default method is the choke point Inventory.addResource
            // merges through, and Inventory and AbstractContainerMenu are the two classes that read the item's
            // own maximum instead.
            // The crafting checkpoints and the punch-to-shear rule: a wrong injection target in any of these is
            // fatal when a player opens a crafting grid or hits a sheep, so they are loaded here instead.
            "net.minecraft.world.inventory.CraftingMenu",
            "net.minecraft.world.entity.animal.Sheep",
            "net.minecraft.world.Container",
            "net.minecraft.world.entity.player.Inventory",
            "net.minecraft.world.inventory.InventoryMenu",
            "net.minecraft.world.inventory.AbstractContainerMenu",
            "net.minecraft.world.inventory.Slot",
            "net.minecraft.server.network.ServerGamePacketListenerImpl");

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

    /**
     * The two block-drop rules that are not ore rules, measured rather than asserted.
     *
     * <h2>Why this probe exists at all — it is the owner's defect, reproduced</h2>
     *
     * <p>Beta 1.9 Pre3's End portal frames were reported to blow up without dropping. The cause was that
     * {@code end_portal_frame} has <b>no loot table</b>, so {@code BlockBehaviour.getDrops} leaves through its first
     * RETURN — the {@code Collections.emptyList()} one — and the injection, written against the post-roll return,
     * never ran. Reading the jar proved the missing loot table; this probe proves the fix, because it calls the
     * same method the explosion path calls and reports what came back.
     *
     * <h2>What this can and cannot prove, stated before the result</h2>
     *
     * <p>The frame drop is fully observable here: it is a pure function of the block and the version, and the
     * explosion that triggers it is the engine's business. So Pre3 must report the frame item and Pre4, Pre5 and
     * every Release must report nothing.
     *
     * <p>The spawner half is read with {@code ItemStack.EMPTY} as the tool, because the probe has no player and no
     * enchanted tool, and inventing one to reach a loot condition would be a different experiment from the one the
     * owner will run. So the POSITIVE half — Pre4's spawner actually dropping under Silk Touch — cannot be observed
     * here and is NOT claimed from this run. What it does catch is the direction that costs real content: no other
     * declared version may report a spawner drop, since the modern game has none and Pre5 removed it. A rule that
     * leaked into its neighbours would show up here as a spawner item in a build that must not have one.
     */
    private static Map<String, Object> blockDropProbe(net.minecraft.server.level.ServerLevel level) {
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("endPortalFrameExplodesGate",
            net.historicalexperience.gameplay.HistoricalGameplayTweaks.endPortalFrameExplodes());
        out.put("silkTouchSpawnerDropGate",
            net.historicalexperience.gameplay.HistoricalGameplayTweaks.silkySpawnerDrops());

        out.put("endPortalFrame", dropReport(level,
            net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME.defaultBlockState(), new BlockPos(24, 90, 24)));
        out.put("monsterSpawner", dropReport(level,
            net.minecraft.world.level.block.Blocks.SPAWNER.defaultBlockState(), new BlockPos(28, 90, 28)));

        return out;
    }

    /** What one block's own drop rules produce right now, through the same entry point an explosion uses. */
    private static Map<String, Object> dropReport(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            level.setBlockAndUpdate(pos, state);
            java.util.List<String> dropped = new java.util.ArrayList<>();
            // No player and no tool: ItemStack.EMPTY, which is also what an explosion carries.
            for (var stack : net.minecraft.world.level.block.Block.getDrops(
                    state, level, pos, null, null, net.minecraft.world.item.ItemStack.EMPTY)) {
                dropped.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                    + " x" + stack.getCount());
            }
            level.removeBlock(pos, false);
            out.put("drops", dropped.isEmpty() ? "<nothing>" : dropped);
            out.put("blockTag", state.getBlock().builtInRegistryHolder().key().location().toString());
        } catch (Throwable failure) {
            out.put("probeFailed", String.valueOf(failure));
        }
        return out;
    }

    /**
     * What the passive spawn path is actually offered in this world, before and after the historical filter.
     *
     * <p>Written for the owner's report that an Alpha world stayed empty after every entity was killed, at tick
     * speed 10000, for hours. The question that settles it is not "is the algorithm right" but "is there anything
     * left to spawn": {@code NaturalSpawner.mobsAt} is the same call the live path makes, so an empty list here
     * means no passive mob can ever appear however long the world runs, and a non-empty one means the fault is
     * further down the chain. Both halves are recorded because a list that is non-empty before the filter and
     * empty after it is a completely different defect from one that is empty in the biome itself.
     */
    private static Map<String, Object> passiveSpawnProbe(net.minecraft.server.level.ServerLevel level,
            String versionId) {
        Map<String, Object> out = new LinkedHashMap<>();
        java.util.Set<String> offered = new java.util.TreeSet<>();
        java.util.Set<String> afterFilter = new java.util.TreeSet<>();
        java.util.Set<String> biomes = new java.util.TreeSet<>();
        var generator = level.getChunkSource().getGenerator();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                var chunk = level.getChunk(x, z);
                for (int px = 0; px < 16; px += 8) {
                    for (int pz = 0; pz < 16; pz += 8) {
                        int wx = x * 16 + px;
                        int wz = z * 16 + pz;
                        int wy = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                            px, pz) + 1;
                        var pos = new BlockPos(wx, wy, wz);
                        var biome = level.getBiome(pos);
                        biomes.add(biome.unwrapKey().orElseThrow().location().toString());
                        // Unfiltered: what the biome itself offers for the passive category.
                        biome.value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE)
                            .unwrap().forEach(data -> offered.add(
                                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                                    .getKey(data.type).toString()));
                        var live = net.historicalexperience.mixin.NaturalSpawnerProbeAccessor.invokeMobsAt(
                            level, level.structureManager(), generator,
                            net.minecraft.world.entity.MobCategory.CREATURE, pos, biome);
                        live.unwrap().forEach(data -> afterFilter.add(
                            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                                .getKey(data.type).toString()));
                    }
                }
            }
        }
        out.put("biomesSampled", biomes);
        out.put("creatureOfferedByBiome", offered);
        out.put("creatureAfterHistoricalFilter", afterFilter);
        out.put("passiveSpawnPossible", !afterFilter.isEmpty());
        out.put("spawnFilterInvocations", net.historicalexperience.gameplay.HistoricalSpawnRules
            .diagnostics().toString());
        return out;
    }

    /** Exercises vanilla Nether generation and the injected spawn-list path, not a table-only analysis. */
    private static Map<String, Object> netherProbe(MinecraftServer server) {
        var level = server.getLevel(net.minecraft.world.level.Level.NETHER);
        if (level == null) throw new IllegalStateException("Nether level missing");
        var generator = level.getChunkSource().getGenerator();
        String alias = HistoricalFeatureExclusions.activeAlias(generator);
        String versionId = alias == null ? null : net.historicalexperience.version.HistoricalVersions
            .byRevisionAlias(alias).orElseThrow().id();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("generator", generator.getClass().getName());
        out.put("ownMarker", HistoricalFeatureExclusions.historicalAlias(generator));
        out.put("effectiveAlias", alias);
        Map<String, Long> blocks = new TreeMap<>();
        java.util.Set<String> candidates = new java.util.TreeSet<>();
        java.util.Set<String> rejectedCandidates = new java.util.TreeSet<>();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                var chunk = level.getChunk(x, z);
                for (var section : chunk.getSections()) {
                    section.getStates().count((state, count) -> blocks.merge(
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                        (long) count, Long::sum));
                }
                for (int y : new int[] {32, 64, 96}) {
                    var pos = new BlockPos(x * 16 + 8, y, z * 16 + 8);
                    for (var category : net.minecraft.world.entity.MobCategory.values()) {
                        var live = net.historicalexperience.mixin.NaturalSpawnerProbeAccessor.invokeMobsAt(
                            level, level.structureManager(), generator, category, pos, level.getBiome(pos));
                        for (var entry : live.unwrap()) {
                            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).toString();
                            candidates.add(id);
                            if (versionId != null && !HistoricalSpawnRules.isAllowed(
                                    HistoricalSpawnRules.forProfile(versionId).orElseThrow(), id)) {
                                rejectedCandidates.add(id);
                            }
                        }
                    }
                }
            }
        }
        if (versionId != null && net.historicalexperience.version.HistoricalVersions.byId(versionId)
                .orElseThrow().chronology() < HistoricalSpawnRules.NETHER_ENDERMAN_CHRONOLOGY && candidates.contains("minecraft:enderman")) {
            rejectedCandidates.add("minecraft:enderman");
        }
        Map<String, Long> failures = new TreeMap<>();
        boolean quartzExcluded = HistoricalFeatureExclusions.runtimeExclusionsForAlias(alias).stream()
            .anyMatch(e -> e.featureId().equals("minecraft:ore_quartz_nether"));
        if (quartzExcluded && blocks.getOrDefault("minecraft:nether_quartz_ore", 0L) > 0) {
            failures.put("minecraft:nether_quartz_ore", blocks.get("minecraft:nether_quartz_ore"));
        }
        blocks.forEach((id, count) -> {
            if (versionId != null && net.historicalexperience.generation.HistoricalBlockContent
                    .isAnachronistic(versionId, id)) failures.put(id, count);
        });
        // Invoke the real placed-feature injection explicitly too. Excluded ore features must return
        // before placement; this catches ore implementations that write chunk sections directly.
        Map<String, Boolean> excludedFeatureResults = new TreeMap<>();
        var registry = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        for (var exclusion : HistoricalFeatureExclusions.runtimeExclusionsForAlias(alias)) {
            if (!java.util.Set.of("minecraft:ore_quartz_nether", "minecraft:ore_gold_nether",
                    "minecraft:ore_magma", "minecraft:ore_ancient_debris_large", "minecraft:ore_debris_small",
                    "minecraft:ore_blackstone", "minecraft:patch_soul_fire").contains(exclusion.featureId())) continue;
            var feature = registry.get(net.minecraft.resources.ResourceLocation.parse(exclusion.featureId()));
            if (feature == null) throw new IllegalStateException("Missing Nether exclusion " + exclusion.featureId());
            long before = HistoricalFeatureExclusions.skippedPlacements();
            boolean placed = feature.placeWithBiomeCheck(level, generator,
                net.minecraft.util.RandomSource.create(1L), new BlockPos(0, 32, 0));
            excludedFeatureResults.put(exclusion.featureId(), placed);
            if (placed || HistoricalFeatureExclusions.skippedPlacements() != before + 1) {
                throw new IllegalStateException("Nether feature bypassed live filter: " + exclusion.featureId());
            }
            boolean nestedPlaced = feature.place(level, generator,
                net.minecraft.util.RandomSource.create(1L), new BlockPos(0, 32, 0));
            if (nestedPlaced || HistoricalFeatureExclusions.skippedPlacements() != before + 2) {
                throw new IllegalStateException("Nested Nether feature bypassed live filter: " + exclusion.featureId());
            }
        }
        out.put("sampledFullChunks", 49);
        out.put("blockCounts", blocks);
        out.put("anachronisticBlocks", failures);
        out.put("liveSpawnCandidates", candidates);
        out.put("anachronisticSpawnCandidates", rejectedCandidates);
        out.put("excludedFeaturePlacementResults", excludedFeatureResults);
        if (!failures.isEmpty() || !rejectedCandidates.isEmpty()) {
            throw new IllegalStateException("Nether enforcement failed: blocks=" + failures + "; mobs=" + rejectedCandidates);
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
            if (Boolean.getBoolean("historical_experience.generationProbe.nether")) {
                report.put("nether", netherProbe(server));
            }
            report.put("features", HistoricalFeatureExclusions.diagnostics());
            report.put("suppressedDecorationBlocks", HistoricalFeatureExclusions.suppressedBlockCount());
            report.put("suppressedDecorationIds", HistoricalFeatureExclusions.suppressedBlockIds());
            // "never asked" vs "asked and allowed": a Beta 1.0 world generated lapis ore and sandstone while the
            // refusal list held only plants, so the veto's coverage had to be measured rather than inferred.
            report.put("blockVetoInvocations", HistoricalFeatureExclusions.vetoInvocations());
            report.put("blockVetoSawIds", new java.util.TreeSet<>(HistoricalFeatureExclusions.seenBlockIds()));
            report.put("ores", BetaOreRules.diagnostics());
            report.put("structureSuppressions", HistoricalStructureRules.suppressedCount());
            report.put("suppressedStructureIds", HistoricalStructureRules.suppressedStructureIds());
            report.put("declaredStructureRuleVersions", HistoricalStructureRules.declaredVersions().toString());
            report.put("allowedStructuresForThisProfile", versionId == null ? "<ordinary world>"
                : HistoricalStructureRules.allowedForVersion(versionId).toString());
            // The village's own reason for refusing, which no other field can show: a world that generates no
            // village and a world whose biomes are wrong look identical from outside, and this tells them apart.
            report.put("villageDeclinedForBiome",
                net.historicalexperience.worldgen.HistoricalVillageStructure.declinedForBiomeCount());
            report.put("villageStyles",
                net.historicalexperience.worldgen.HistoricalVillagePieces.styleDiagnostics());
            report.put("spawns", HistoricalSpawnRules.diagnostics());
            report.put("passiveSpawn", passiveSpawnProbe(level, versionId));
            // An invalid id here DELETES a mob rather than failing to allow one, because the filter removes 
            // every type absent from the allowed set. This is checked against the entity registry, not against a 
            // transcription of it.
            report.put("spawnIdsUnknown", HistoricalSpawnRules.unknownEntityIds());
            // The mob-drop replacement and experience suppression, so a real session shows them working
            // instead of only a unit test asserting the tables are readable.
            // The live observation of the simulation rules, which the block and structure checks cannot see.
            report.put("gameplay", gameplayProbe(level, versionId));
            report.put("oreDrops", oreDropProbe(level));
            report.put("blockDrops", blockDropProbe(level));
            report.put("damageTable", damageTableProbe());
            report.put("toolDurability", toolDurabilityProbe(versionId));
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