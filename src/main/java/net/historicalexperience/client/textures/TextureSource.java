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
package net.historicalexperience.client.textures;

/**
 * Every source the historical texture stack can draw a resource from, lowest priority first.
 *
 * <p>The {@link #rank()} is the effective priority inside the historical group and is the order the layers are
 * stacked in: <b>Golden Days Alpha &gt; Golden Days Base &gt; Programmer Art Continuation &gt; Programmer Art &gt;
 * vanilla</b>. The whole group is then placed above the user's selected packs (see {@link HistoricalTextureService}).
 *
 * <p>{@link #VANILLA_PIN} is not a dependency. It is the game's own resources, admitted into the group only for the
 * handful of paths a rule explicitly pins to vanilla, so that a path the historical system has decided about cannot be
 * overridden by a user pack even when no historical pack supplies it (the Golden Days XP bar is the case: Programmer
 * Art has no XP-bar sprites in 1.21.1, so after Golden Days is excluded the answer is vanilla's).
 */
public enum TextureSource {
    VANILLA_PIN(0, "vanilla", false),
    PROGRAMMER_ART(1, "programmer_art", true),
    CONTINUATION(2, "continuation", true),
    GOLDEN_DAYS_BASE(3, "golden_days_base", true),
    GOLDEN_DAYS_ALPHA(4, "golden_days_alpha", true);

    private final int rank;
    private final String id;
    private final boolean dependency;

    TextureSource(int rank, String id, boolean dependency) {
        this.rank = rank;
        this.id = id;
        this.dependency = dependency;
    }

    /** Priority inside the historical group; higher wins. */
    public int rank() {
        return rank;
    }

    /** Stable machine id, used in pack ids, logs and the config key. */
    public String id() {
        return id;
    }

    /** True for the four packs the player must have, false for the vanilla pin. */
    public boolean isDependency() {
        return dependency;
    }

    public String translationKey() {
        return "historical_experience.textures.source." + id;
    }
}
