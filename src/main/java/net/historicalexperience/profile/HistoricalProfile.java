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
