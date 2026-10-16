/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world.upgrade;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import net.historicalexperience.version.HistoricalVersions;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.historicalexperience.world.metadata.MetadataException;

/** Binds a finite transition to its saved seed AND immutable historical origin, not a directory name or active session.
 * Copies of the same world remain valid; an unrelated world with the same seed does not inherit the plan.
 * Source geometry is proven by the three preset-only settings references, never inferred from a border size.
 */
public record FiniteTransitionIdentity(Long worldSeed, String originHash, String sourcePreset, FiniteGeometry geometry) {
    public FiniteTransitionIdentity {
        if (worldSeed == null || originHash == null || !originHash.matches("[0-9a-f]{64}")
                || sourcePreset == null || !HistoricalVersions.isFinitePreset(sourcePreset)) {
            throw new IllegalArgumentException("Invalid finite transition identity");
        }
        // A plan written before the geometry was recorded carries no field; it can only have come from a world the old
        // rule accepted, which was the preset-only default, so that is what its absence means.
        geometry = geometry == null ? FiniteGeometry.DEFAULT : geometry;
    }

    /** The source must still be stored as finite; called before staging or changing any region bytes. */
    public static FiniteTransitionIdentity fromSource(Path world) throws IOException {
        try {
            var state = WorldGenSettingsAdvance.read(world);
            if (state.isEmpty()) {
                throw new UpgradeException("No stored historical finite generator");
            }
            return new FiniteTransitionIdentity(WorldGenSettingsAdvance.readWorldSeed(world), originHash(world),
                state.get().settingsPreset(), WorldGenSettingsAdvance.readFiniteGeometry(world));
        } catch (UpgradeException failure) {
            // The cause is what the player sees on the "Converting world biomes" screen, so carry it: the generic
            // wrapper alone told the owner nothing about why their world could not advance.
            String cause = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            throw new IOException("Cannot establish safe default finite source identity: " + cause, failure);
        }
    }

    /** Applies equally before and after the generator advances. Metadata revision/preset changes do not change origin. */
    public void verifyWorld(Path world) throws IOException {
        try {
            verifySeed(WorldGenSettingsAdvance.readWorldSeed(world));
        } catch (UpgradeException failure) {
            throw new IOException("Cannot verify finite transition seed", failure);
        }
        if (!originHash.equals(originHash(world))) {
            throw new IOException("Finite transition belongs to a different historical world origin; refusing to apply it");
        }
    }

    public void verifySeed(long actual) throws IOException {
        if (worldSeed.longValue() != actual) {
            throw new IOException("Finite transition belongs to a different world seed; refusing to apply it");
        }
    }

    private static String originHash(Path world) throws IOException {
        try {
            var metadata = HistoricalMetadataStore.forWorldDirectory(world).read();
            var first = metadata.history().getFirst();
            if (first.revision() != 1) throw new IOException("Historical origin revision 1 is missing");
            String origin = new Gson().toJson(first);
            return WorldBackup.sha256(origin.getBytes(StandardCharsets.UTF_8));
        } catch (MetadataException failure) {
            throw new IOException("Cannot verify finite transition historical origin", failure);
        }
    }
}
