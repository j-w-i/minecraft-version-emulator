package net.historicalexperience.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the {@link LevelSummary} behind a world-list row.
 *
 * <p>Needed because the row's own accessor is misleading for this purpose: {@code getLevelName()} returns
 * {@code summary.getLevelName()}, the name a player sees, not the folder a world lives in. Two worlds may share a
 * display name, so resolving a save directory from it would be able to pick the wrong world - and this code
 * changes world metadata, which makes picking the wrong one unacceptable.
 *
 * <p>{@code LevelSummary.getLevelId()} is the folder name, and it is public; only the field holding the summary is
 * not.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(WorldSelectionList.WorldListEntry.class)
public interface WorldListEntryAccessor {
    /** The summary of the world this row represents. */
    @Accessor("summary")
    LevelSummary historicalExperience$summary();
}
