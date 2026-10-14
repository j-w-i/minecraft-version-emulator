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
package net.historicalexperience.world.metadata;

/**
 * Persisted metadata schema registry.
 *
 * <p>The schema version is stored explicitly in the metadata document. Only
 * {@link #CURRENT} is accepted by this build; anything else (in particular a
 * higher version written by a newer build) must be treated as unreadable rather
 * than repaired in place, so that unsupported stored content is preserved.
 */
public final class MetadataSchema {
    /** The schema version written and read by this build. */
    public static final int CURRENT = 1;

    /** Relative file name of the metadata document inside a world directory. */
    public static final String FILE_NAME = "historical_experience_metadata.json";

    private MetadataSchema() {
    }
}
