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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Alpha v1.2.2a F4 key: rebuild a lit Nether portal beside the player.
 *
 * <h2>What the original did, and where this was read from</h2>
 *
 * <p>Alpha v1.2.2a's client handled key code 62 by calling {@code new nj().c(world, player)}. Its
 * {@code Minecraft.java:829} is the whole trigger:
 * {@code if (Keyboard.getEventKey() == 62) { new nj().c(this.e, this.g); continue; }}. Both that client and Alpha
 * 1.2.0 were downloaded and SHA-1 verified against Mojang's manifest and decompiled; 1.2.0 has no F4 case.
 *
 * <p>{@code nj.c} searched 16 blocks outward for a column whose top solid block had clear space above it, took an
 * orientation from {@code random.nextInt(4)}, and scored candidates by squared distance to the player so the nearest
 * won. It then set {@code world.i = true} around the placement - the flag that makes a portal block place LIT rather
 * than as an empty frame - and wrote, for {@code w} running over the four columns and {@code d} over the five rows:
 *
 * <pre>
 *   frame  where w is -1 or 2, or d is -1 or 3   (obsidian)
 *   portal where w is 0 or 1 AND d is 0, 1 or 2  (lit portal block)
 * </pre>
 *
 * <p>That is a <b>4x5 obsidian frame with a 2x3 opening</b>, which is the same shape the game's own portal builder
 * produces and the smallest working Nether portal. It is also why F4 read as "spawn a portal" rather than "teleport":
 * {@code nj.b}, the method that looks for an existing portal within 128 blocks and moves the player to it, is never
 * called from the F4 path.
 *
 * <h2>What is deliberately different</h2>
 *
 * <p>The original chose its axis at random and would happily bury a portal in a hillside, because it only checked the
 * volume it was about to write. This checks the volume is clear first, so a portal is only built somewhere it fits,
 * and derives the axis from the way the player is looking - a debug key that silently does nothing is worse than one
 * that puts the portal where the player can see it. The frame, the opening and the "lit" outcome are unchanged.
 */
public final class HistoricalPortalSpawn {
    /** Alpha v1.2.2a searched this far out from the player. */
    private static final int SEARCH_RADIUS = 16;

    /** The frame is four wide and five tall, counting the obsidian; the opening inside it is two by three. */
    private static final int FRAME_WIDTH = 4;
    private static final int FRAME_HEIGHT = 5;

    private HistoricalPortalSpawn() {
    }

    /**
     * Builds a lit portal near the player if the version has the key.
     *
     * @return true when a portal was placed, so a caller can report "nothing happened" honestly rather than silently
     */
    public static boolean spawnNear(ServerLevel level, ServerPlayer player) {
        if (!Features.active(Feature.F4_PORTAL_KEY, HistoricalProfileLookup.profileIdForLevel(level))) {
            return false;
        }
        Direction.Axis axis = axisFor(player);
        BlockPos origin = findOrigin(level, player, axis);
        if (origin == null) {
            return false;
        }
        return build(level, origin, axis);
    }

    /** The internal entry point, kept so both the key and the probe reach the same writing path. */
    private static boolean build(ServerLevel level, BlockPos origin, Direction.Axis axis) {
        return buildAt(level, origin, axis) > 0;
    }

    /**
     * The orientation of the portal plane: the one the player is more nearly facing edge-on.
     *
     * <p>{@code AXIS} names the axis the portal block lies ALONG, so a player looking down X gets a portal on the X
     * axis - flat across their view - and one looking down Z gets the Z axis.
     */
    private static Direction.Axis axisFor(ServerPlayer player) {
        return Math.abs(player.getLookAngle().x) >= Math.abs(player.getLookAngle().z)
            ? Direction.Axis.X
            : Direction.Axis.Z;
    }

    /** The nearest block position at which the whole frame fits with a floor under it, or null if none does. */
    private static BlockPos findOrigin(ServerLevel level, ServerPlayer player, Direction.Axis axis) {
        BlockPos feet = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -2; dy <= 6; dy++) {
                    BlockPos candidate = feet.offset(dx, dy, dz);
                    if (!fits(level, candidate, axis)) {
                        continue;
                    }
                    // Squared distance to the player's feet, as the original scored its candidates.
                    double distance = (double) dx * dx + (double) dy * dy + (double) dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Whether the frame fits at {@code origin}: a solid floor beneath the whole width, and every cell of the 4x5
     * volume clear.
     *
     * <p>The original checked only that the frame's own cells were air or replaceable. Requiring the volume to be clear
     * is the difference described in the class comment, and it is what stops F4 from overwriting the wall of a house.
     *
     * <p>The axis is a parameter because the frame is laid along it: checking a Z-axis frame with X-axis coordinates
     * would approve a space the build then writes somewhere else.
     */
    private static boolean fits(ServerLevel level, BlockPos origin, Direction.Axis axis) {
        for (int w = -1; w < FRAME_WIDTH - 1; w++) {
            BlockPos floor = offset(origin, axis, w, -2);
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return false;
            }
            for (int d = -1; d < FRAME_HEIGHT - 1; d++) {
                if (!clear(level, offset(origin, axis, w, d))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Air or a replaceable plant, which the portal may overwrite. */
    private static boolean clear(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    /**
     * Builds the portal at a given place, for a caller that has already decided where and which way.
     *
     * <h2>Why the frame goes down before a single portal block</h2>
     *
     * <p>{@code NetherPortalBlock.updateShape} asks {@code PortalShape.isComplete()} whether the frame around it is
     * whole, and turns the block back to air when it is not. Writing frame and opening in one pass therefore destroys
     * the portal as it is built: each portal block is placed while the frame beside it is still missing, fails the
     * check, and is gone by the time the next one lands. That was measured, not assumed - the first runtime probe read
     * back 14 obsidian and six blocks of AIR where the opening should have been.
     *
     * <p>So the obsidian goes down first and is finished before any portal block exists, and each portal block then
     * sees a complete frame. The blocks are written with neighbour updates because that is what the game's own portal
     * builder does; the ordering is what makes it work.
     *
     * @return the number of blocks written
     */
    public static int buildAt(ServerLevel level, BlockPos origin, Direction.Axis axis) {
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState portal = Blocks.NETHER_PORTAL.defaultBlockState()
            .setValue(net.minecraft.world.level.block.NetherPortalBlock.AXIS, axis);
        java.util.List<Slot> slots = layout(origin, axis);
        int written = 0;
        // Frame first, whole, so isComplete() can succeed for everything placed after it.
        for (Slot slot : slots) {
            if (slot.frame()) {
                level.setBlock(slot.pos(), obsidian, 3);
                written++;
            }
        }
        for (Slot slot : slots) {
            if (!slot.frame()) {
                level.setBlock(slot.pos(), portal, 3);
                written++;
            }
        }
        return written;
    }

    /** One position of the portal, and whether the frame or the opening goes there. */
    public record Slot(BlockPos pos, boolean frame) {
    }

    /**
     * The portal's shape as positions and a frame flag, with no blocks and no world involved.
     *
     * <p>{@code w} runs over the four columns and {@code d} over the five rows. A cell is frame when it is an outer
     * column or an outer row, and opening when it is in neither, which leaves 2 by 3.
     *
     * <p>Deliberately free of {@code Blocks}: touching a block class pulls in the whole registry, which does not exist
     * in a unit test, so a shape expressed in block states could only be checked by placing one in a running game and
     * looking at it. This way the geometry is asserted directly - 14 frame cells, 6 opening cells, four wide, five
     * tall, the opening never on the border - and only the choice of block is left to the runtime path.
     */
    public static java.util.List<Slot> layout(BlockPos origin, Direction.Axis axis) {
        java.util.List<Slot> slots = new java.util.ArrayList<>(FRAME_WIDTH * FRAME_HEIGHT);
        for (int w = -1; w < FRAME_WIDTH - 1; w++) {
            for (int d = -1; d < FRAME_HEIGHT - 1; d++) {
                boolean frame = w == -1 || w == FRAME_WIDTH - 2 || d == -1 || d == FRAME_HEIGHT - 2;
                slots.add(new Slot(offset(origin, axis, w, d), frame));
            }
        }
        return java.util.List.copyOf(slots);
    }

    /** The frame is four wide and five tall; exposed so a test can state the size rather than infer it. */
    public static int frameWidth() {
        return FRAME_WIDTH;
    }

    /** The frame's height in blocks. */
    public static int frameHeight() {
        return FRAME_HEIGHT;
    }

    /** Turns the frame's own (width, height) coordinates into a world position along the chosen axis. */
    private static BlockPos offset(BlockPos origin, Direction.Axis axis, int w, int d) {
        return axis == Direction.Axis.X
            ? origin.offset(w, d, 0)
            : origin.offset(0, d, w);
    }
}
