package net.historicalexperience.gameplay;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Opening a door, throwing a lever and pressing a button with a LEFT click, which the modern game does with a right
 * click.
 *
 * <h2>Source and boundary</h2>
 *
 * <p>Ported from Nostalgic Tweaks {@code listener/common/InteractionListener.onLeftClickBlock}, which answers
 * {@code LEFT_CLICK_DOOR}, {@code LEFT_CLICK_LEVER} and {@code LEFT_CLICK_BUTTON} (LGPL-3.0; see
 * THIRD_PARTY_NOTICES.md). Upstream's conditions - survival or adventure only, and only while the player is not
 * already swinging - are kept, because a creative player left-clicking is destroying the block, not using it.
 *
 * <p><b>The boundary is the owner's: left-click activation was removed in Release 1.4.2.</b> The declared catalogue
 * has a Release 1.4 profile and no 1.4.1 or 1.4.2 profile, so the rule is placed at that profile's end: versions
 * before it keep left-click activation, Release 1.4 still has it (1.4 and 1.4.1 did), and Release 1.5 onwards does
 * not. That is the same "too early is safe" direction the rest of this project uses for a boundary that falls
 * between declared builds - rounding the other way would delete the behaviour from a build that had it.
 *
 * <h2>Why the attack callback, and why it is not server-only</h2>
 *
 * <p>{@code AttackBlockCallback} is Fabric's left-click-on-a-block event. The same lesson as the axe rule applies:
 * the client runs this too, and a handler that acts on the server alone lets the client predict a swing the server
 * then contradicts. Here the action is a block-state change, so it is performed on the server and the callback
 * answers PASS on both sides - returning a non-PASS result would also suppress the ordinary block breaking that a
 * left click is supposed to do when the block is not one of these.
 */
public final class HistoricalLeftClickEvents {
    private HistoricalLeftClickEvents() {
    }

    /** Left-click activation survived until Release 1.4.2; the first declared build without it is Release 1.5. */
    private static final String FIRST_WITHOUT_LEFT_CLICK = "historical_experience:release_1_5";

    public static void register() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!availableFor(HistoricalClientRules.activeVersion())
                    || player.isCreative()
                    || player.swinging
                    || !(level.getBlockState(pos).getBlock() instanceof DoorBlock
                        || level.getBlockState(pos).getBlock() instanceof TrapDoorBlock
                        || level.getBlockState(pos).getBlock() instanceof FenceGateBlock
                        || level.getBlockState(pos).getBlock() instanceof LeverBlock
                        || level.getBlockState(pos).getBlock() instanceof ButtonBlock)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                use(level, player, pos, direction);
            }
            // PASS, so the left click still does everything else it normally does.
            return InteractionResult.PASS;
        });
    }

    /** The same activation a right click would perform. */
    private static void use(Level level, Player player, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), direction, pos, false);
        state.useWithoutItem(level, player, hit);
    }

    /** Whether this version still activated these blocks with a left click. */
    public static boolean availableFor(HistoricalVersion version) {
        if (version == null) {
            return false;
        }
        return HistoricalVersions.byId(FIRST_WITHOUT_LEFT_CLICK)
            .map(first -> version.chronology() < first.chronology())
            .orElse(false);
    }
}
