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

import java.util.List;

/**
 * One dependency pack pinned to an exact published release.
 *
 * <p>Every value below was read from Modrinth's public API ({@code api.modrinth.com/v2/version/<id>}) for that exact
 * version, not from a "latest" query, so the download is reproducible and a changed or tampered file is rejected: the
 * SHA-512 is checked before a downloaded archive is allowed to replace anything. The URLs are Modrinth's own CDN.
 *
 * <p>The three packs are All Rights Reserved. They are therefore fetched on the player's own machine from their
 * official distribution point, exactly as a launcher would, and are never bundled in this mod, copied into this
 * repository, or re-hosted.
 */
public record PackPin(TextureSource source, String projectSlug, String projectId, String versionId,
                      String versionNumber, String fileName, String url, String sha512, long size) {

    public static final PackPin GOLDEN_DAYS_BASE = new PackPin(
        TextureSource.GOLDEN_DAYS_BASE, "golden-days", "BFzJ6aQL", "rhfgMfhx", "16.5",
        "golden-days-base-16.5-[1.20-to-26.4].zip",
        "https://cdn.modrinth.com/data/BFzJ6aQL/versions/rhfgMfhx/golden-days-base-16.5-%5B1.20-to-26.4%5D.zip",
        "d199da92241ae0b9a1fcdee3499a528a81b3242a1b40477f4149ab3543893df1"
            + "8acd70776c80bc8c7ee8e7a99fa312eb461c92b0191bb1b3dc3d45e2ee895d21",
        5_580_808L);

    public static final PackPin GOLDEN_DAYS_ALPHA = new PackPin(
        TextureSource.GOLDEN_DAYS_ALPHA, "golden-days-alpha", "GalExSh7", "hIV3CAmq", "16.5",
        "golden-days-alpha-16.5-[1.20-to-26.4].zip",
        "https://cdn.modrinth.com/data/GalExSh7/versions/hIV3CAmq/golden-days-alpha-16.5-%5B1.20-to-26.4%5D.zip",
        "6741d1250a931102acb48adc91b1ede706fcecb19371d0b2d13185ed281dbf65"
            + "751c16af72fb681c007db298bc59c397382aa81a7a28bf0c1cf7270a35ce5497",
        375_306L);

    public static final PackPin CONTINUATION = new PackPin(
        TextureSource.CONTINUATION, "pacp", "gx2HeuWD", "7sZqSYLJ", "2.5.4",
        "PACP_Base_Release.zip",
        "https://cdn.modrinth.com/data/gx2HeuWD/versions/7sZqSYLJ/PACP_Base_Release.zip",
        "9c72b9e23e7dbd4c99f0570707f45f815e6129c6b64b37e9999de010c38b995d"
            + "58bdfa723ef71d49dc8a1eee8c58f7a0f028d0388a49e2ff7acd156b78472837",
        4_513_210L);

    public static final List<PackPin> ALL = List.of(GOLDEN_DAYS_BASE, GOLDEN_DAYS_ALPHA, CONTINUATION);

    public static PackPin of(TextureSource source) {
        for (PackPin pin : ALL) {
            if (pin.source == source) {
                return pin;
            }
        }
        return null;
    }

}
