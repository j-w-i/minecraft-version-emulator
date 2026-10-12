package net.historicalexperience.profile;

import java.util.Objects;

import net.historicalexperience.world.metadata.HistoricalIdentifier;

/**
 * An immutable, resolved historical gameplay profile.
 *
 * <p>A profile is the unit of historical semantics: it names an era of the game and pins the
 * terrain generation preset that backs it. It is deliberately separate from
 * {@link #generationPreset()}: the profile identifier is what gameplay rules will key off, while
 * the generation preset identifier is what terrain keys off. Keeping them in one immutable value
 * makes it impossible to end up with a profile whose terrain binding has drifted.
 *
 * @param id               profile identifier
 * @param displayName      human readable name, never null
 * @param generationPreset the terrain preset that backs this profile
 */
public record HistoricalProfile(
    HistoricalIdentifier id,
    String displayName,
    HistoricalIdentifier generationPreset
) {
    public HistoricalProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(generationPreset, "generationPreset");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}
