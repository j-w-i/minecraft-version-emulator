/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.gameplay;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EarlyMobDropFactsTest {
    private static final Map<String, String> PROFILES = Map.of(
        "in-20100223.jar", "historical_experience:indev_latest",
        "inf-20100420.jar", "historical_experience:infdev_420",
        "inf-20100611.jar", "historical_experience:infdev_611");
    private static List<JsonObject> cases() throws Exception {
        var data = JsonParser.parseString(Files.readString(Path.of("research/early-mob-drop-facts.json"))).getAsJsonObject();
        var rows = new ArrayList<JsonObject>(); data.getAsJsonArray("cases").forEach(value -> rows.add(value.getAsJsonObject())); return rows;
    }
    @Test void exactArtifactIdentitiesAndSelectorShapes() throws Exception {
        var sha = Map.of("in-20100223.jar", "801ce486bb7fd1b43a56bc5d226dfb1370c08678",
            "inf-20100420.jar", "27010a5137abd2c8d8df85e99c14f5406ec197b3",
            "inf-20100611.jar", "421318a554f17463a56a271d08e9597941d066d9");
        var size = Map.of("in-20100223.jar", 555633, "inf-20100420.jar", 552012, "inf-20100611.jar", 566417);
        var seen = new HashSet<String>();
        for (var row : cases()) {
            String artifact = row.get("artifact").getAsString(); assertTrue(seen.add(artifact));
            assertEquals(sha.get(artifact), row.get("sha1").getAsString()); assertEquals(size.get(artifact).intValue(), row.get("bytes").getAsInt());
            assertEquals(6, row.getAsJsonArray("selectors").size());
            for (var selector : row.getAsJsonArray("selectors")) assertEquals(artifact.startsWith("in-") ? "c()I" : "b()I", selector.getAsJsonObject().get("selector").getAsString());
        }
        assertEquals(PROFILES.keySet(), seen);
    }
    @Test void numericItemsAndBoundsMatchEveryEarlyProfile() throws Exception {
        var items = Map.of(288, "feather", 262, "arrow", 319, "porkchop", 287, "string", 289, "gunpowder");
        for (var row : cases()) {
            String profile = PROFILES.get(row.get("artifact").getAsString()); var seen = new HashSet<String>();
            for (var value : row.getAsJsonArray("selectors")) {
                var selector = value.getAsJsonObject(); String entity = selector.get("entity").getAsString(); assertTrue(seen.add(entity));
                int item = selector.get("numericDrop").getAsInt();
                if (entity.equals("sheep")) continue;
                assertTrue(items.containsKey(item)); var rule = HistoricalLootRules.forEntity(profile, "minecraft:" + entity).orElseThrow();
                assertEquals("minecraft:" + items.get(item), rule.itemId()); assertEquals(3, rule.maxExclusiveCount());
            }
            assertEquals(Set.of("zombie", "skeleton", "pig", "spider", "creeper", "sheep"), seen);
        }
    }
    @Test void sheepHasNoDeathItemInAllThreeArtifacts() throws Exception {
        for (var row : cases()) for (var value : row.getAsJsonArray("selectors")) {
            var selector = value.getAsJsonObject(); if (!selector.get("entity").getAsString().equals("sheep")) continue;
            assertEquals(0, selector.get("numericDrop").getAsInt());
            assertEquals(row.get("artifact").getAsString().startsWith("in-") ? "net.minecraft.a.c.e" : "net.minecraft.a.c.f", selector.get("selectorOwner").getAsString());
            assertTrue(HistoricalLootRules.forEntity(PROFILES.get(row.get("artifact").getAsString()), "minecraft:sheep").isEmpty());
        }
    }
    /**
     * The death-wool window is Beta 1.7 to Release 1.8, not "everything before 1.8".
     *
     * <p>Corrected by the 2026-10-21 version audit: minecraft.wiki, Sheep, has the death drop arriving in Beta 1.7
     * ("Sheep no longer drop wool when hit, albeit they drop one wool when being killed when they are unsheared"),
     * so an Alpha or Beta 1.0-1.6 sheep must give nothing when killed. The old assertion encoded the wider window.
     */
    @Test void sheepDeathWoolWindowIsBeta17ToRelease18() {
        int from = HistoricalVersions.byId("historical_experience:beta_1_7_3").orElseThrow().chronology();
        int until = HistoricalVersions.chronology(HistoricalVersions.ERA_RELEASE, 1, 8, 0);
        for (var version : HistoricalVersions.all()) {
            boolean expected = version.chronology() >= from && version.chronology() < until;
            org.junit.jupiter.api.Assertions.assertEquals(expected, HistoricalLootRules.usesLegacySheepDrops(version.id()),
                version.id() + " sheep death wool");
        }
        // Beta 1.7.3 is the first declared build in the window; earlier declared builds are not.
        assertTrue(HistoricalLootRules.usesLegacySheepDrops("historical_experience:beta_1_7_3"));
        assertFalse(HistoricalLootRules.usesLegacySheepDrops("historical_experience:beta_1_6"));
    }

    @Test void ordinaryUnknownAndFutureSheepDoNotAcquireOldTables() {
        for (String profile : Arrays.asList(null, "minecraft:normal", "unknown", "historical_experience:release_1_8_9", "historical_experience:indev_latest_with_ores")) {
            assertFalse(HistoricalLootRules.usesLegacySheepDrops(profile));
        }
    }
}
