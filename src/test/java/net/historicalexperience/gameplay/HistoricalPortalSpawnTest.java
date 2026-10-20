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

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The F4 portal's SHAPE, asserted directly rather than by placing one and looking at it.
 *
 * <p>Read from Alpha v1.2.2a's own client: for {@code w} over four columns and {@code d} over five rows, a cell is
 * frame where {@code w} is -1 or 2 or {@code d} is -1 or 3, and opening otherwise. That is a 4x5 obsidian frame with a
 * 2x3 lit opening, which is the smallest working Nether portal and the same shape the game's own builder makes.
 *
 * <p>These assertions use {@code layout}, which is free of block classes on purpose: a block class pulls in the whole
 * registry, which a unit test does not have, so anything expressed in block states could only be checked in a running
 * game. The shape is the part that can be wrong quietly, so it is the part tested here.
 */
class HistoricalPortalSpawnTest {
    private static final BlockPos ORIGIN = new BlockPos(100, 64, 200);

    @Test
    @DisplayName("the portal is a 4x5 frame with a 2x3 opening, exactly as the original wrote it")
    void frameAndOpening() {
        List<HistoricalPortalSpawn.Slot> slots = HistoricalPortalSpawn.layout(ORIGIN, Direction.Axis.X);

        assertEquals(20, slots.size(), "every cell of the 4x5 frame is written");
        assertEquals(HistoricalPortalSpawn.frameWidth() * HistoricalPortalSpawn.frameHeight(), slots.size());

        long frames = slots.stream().filter(HistoricalPortalSpawn.Slot::frame).count();
        assertEquals(14, frames, "the border is 14 blocks of obsidian");
        assertEquals(6, slots.size() - frames, "and the opening is 2 wide by 3 tall");
    }

    @Test
    @DisplayName("the two axes lay the frame along themselves, so a portal is never built the wrong way round")
    void axisLaysTheFrameAlongItself() {
        for (Direction.Axis axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
            List<HistoricalPortalSpawn.Slot> slots = HistoricalPortalSpawn.layout(ORIGIN, axis);
            if (axis == Direction.Axis.X) {
                assertTrue(slots.stream().allMatch(slot -> slot.pos().getZ() == ORIGIN.getZ()),
                    "an X-axis portal stays in one Z plane");
                assertTrue(slots.stream().anyMatch(slot -> slot.pos().getX() != ORIGIN.getX()),
                    "and extends along X");
            } else {
                assertTrue(slots.stream().allMatch(slot -> slot.pos().getX() == ORIGIN.getX()),
                    "a Z-axis portal stays in one X plane");
                assertTrue(slots.stream().anyMatch(slot -> slot.pos().getZ() != ORIGIN.getZ()),
                    "and extends along Z");
            }
        }
    }

    @Test
    @DisplayName("the opening never touches the outer border, and the frame never intrudes into it")
    void openingIsCentred() {
        List<HistoricalPortalSpawn.Slot> slots = HistoricalPortalSpawn.layout(ORIGIN, Direction.Axis.X);
        int minX = slots.stream().mapToInt(s -> s.pos().getX()).min().orElseThrow();
        int maxX = slots.stream().mapToInt(s -> s.pos().getX()).max().orElseThrow();
        int minY = slots.stream().mapToInt(s -> s.pos().getY()).min().orElseThrow();
        int maxY = slots.stream().mapToInt(s -> s.pos().getY()).max().orElseThrow();

        assertEquals(4, maxX - minX + 1, "four columns wide");
        assertEquals(5, maxY - minY + 1, "five rows tall");

        for (HistoricalPortalSpawn.Slot slot : slots) {
            if (slot.frame()) {
                continue;
            }
            assertTrue(slot.pos().getX() > minX && slot.pos().getX() < maxX,
                "an opening cell sits inside the outer columns: " + slot.pos());
            assertTrue(slot.pos().getY() > minY && slot.pos().getY() < maxY,
                "and inside the outer rows: " + slot.pos());
        }
    }

    @Test
    @DisplayName("every position is written once, so no cell is overwritten by another")
    void noDuplicatePositions() {
        List<HistoricalPortalSpawn.Slot> slots = HistoricalPortalSpawn.layout(ORIGIN, Direction.Axis.Z);
        assertEquals(slots.size(), slots.stream().map(HistoricalPortalSpawn.Slot::pos).distinct().count(),
            "a duplicated position would mean the frame and the opening disagree about a block");
    }

    @Test
    @DisplayName("only Alpha v1.2.2a has the key, and the boundary is stated against the registry")
    void onlyTheOneBuildHasTheKey() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            boolean expected = "historical_experience:alpha_1_2_2a".equals(version.id());
            assertEquals(expected, Features.active(Feature.F4_PORTAL_KEY, version.id()),
                version.id() + ": the F4 portal key belongs to exactly one declared build");
        }
        assertEquals(HistoricalVersions.byId("historical_experience:alpha_1_2_2a").orElseThrow().chronology(),
            HistoricalGameplayTweaks.f4PortalBoundaryChronology(),
            "the literal and the registry must agree, or the gate could name the wrong build");
    }

    @Test
    @DisplayName("an ordinary world and an unknown id never have the key")
    void ordinaryWorldHasNoKey() {
        assertFalse(Features.active(Feature.F4_PORTAL_KEY, (String) null));
        assertFalse(Features.active(Feature.F4_PORTAL_KEY, "historical_experience:no_such_version"));
        assertFalse(Features.active(Feature.F4_PORTAL_KEY, "minecraft:overworld"),
            "a non-historical id must not answer true");
    }

    @Test
    @DisplayName("Alpha 1.2.0 does NOT have it, which is the whole reason this build exists")
    void alpha120DoesNotHaveTheKey() {
        assertFalse(Features.active(Feature.F4_PORTAL_KEY, "historical_experience:alpha_1_2_0"),
            "Alpha 1.2.0's client has no F4 case; only 1.2.2a does");
        assertTrue(Features.active(Feature.F4_PORTAL_KEY, "historical_experience:alpha_1_2_2a"));
        // The Nether is a wider boundary than this key, and the two must not be conflated.
        assertTrue(!Features.active(Feature.NO_NETHER, "historical_experience:alpha_1_2_0"),
            "1.2.0 has the Nether; it just has no F4 key");
        assertTrue(HistoricalGameplayTweaks.f4PortalBoundaryChronology()
            > HistoricalGameplayTweaks.netherBoundaryChronology(),
            "the key's boundary is later than the Nether's and is stated separately");
    }
}