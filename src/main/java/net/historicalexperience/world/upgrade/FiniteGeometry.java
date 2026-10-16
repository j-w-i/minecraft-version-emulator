/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world.upgrade;

/**
 * The finite map geometry a world actually generates with, read from its own stored Moderner Beta settings.
 *
 * <h2>Why this exists</h2>
 *
 * <p>The apron migration used to assume the default Indev map - a 256x256 footprint treated as chunks [-8,8), a
 * 128-block envelope, and a shell of bedrock/dirt/water - and refused any world whose settings were customized. That
 * refusal was honest about the geometry, but it also meant a world with, say, an Inland map or a smaller footprint
 * could never migrate at all. Recording the geometry the world really has, and proving it is well formed, is what
 * lets the migration work for those worlds without guessing.
 *
 * <p>What it is NOT: a promise that this project reproduces Moderner Beta's terrain. These are the four values the
 * settings component itself carries, plus the type and theme ids, handed back verbatim.
 *
 * @param width  blocks along X
 * @param length blocks along Z
 * @param height blocks the terrain parameter, as stored; the level envelope is the profile's own
 * @param type   the stored level type id ({@code island}, {@code floating}, {@code inland}, {@code classic})
 * @param theme  the stored theme id ({@code normal}, {@code hell}, {@code paradise}, {@code woods})
 * @param customized whether the world recorded these values itself, i.e. not the shipped default preset
 */
public record FiniteGeometry(int width, int length, int height, String type, String theme, boolean customized) {
    /** The geometry the project's own Indev preset ships: what a world with preset-only settings generates. */
    public static final FiniteGeometry DEFAULT = new FiniteGeometry(256, 256, 128, "island", "normal", false);

    /** Bounds are sanity limits, not fidelity claims: anything outside them is a malformed or foreign settings file. */
    private static final int MIN_SIDE = 16;
    private static final int MAX_SIDE = 4096;
    private static final int MIN_HEIGHT = 16;
    private static final int MAX_HEIGHT = 512;

    public FiniteGeometry {
        if (width < MIN_SIDE || width > MAX_SIDE || length < MIN_SIDE || length > MAX_SIDE) {
            throw new IllegalArgumentException("Finite map side out of range: " + width + "x" + length);
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            throw new IllegalArgumentException("Finite map height out of range: " + height);
        }
        if (type == null || type.isBlank() || theme == null || theme.isBlank()) {
            throw new IllegalArgumentException("Finite map settings must name a type and a theme");
        }
    }

    /**
     * Whether a chunk is part of the island, i.e. carries the map's own terrain and must never be released.
     *
     * <p>The map is centred on the origin, so the blocks are X in [-width/2, width/2) and Z in [-length/2, length/2).
     * For the default 256 that is exactly chunks [-8,8) - the range the migration used to hardcode.
     */
    public boolean inside(int chunkX, int chunkZ) {
        return chunkX >= Math.floorDiv(-(width / 2), 16) && chunkX <= Math.floorDiv(width / 2 - 1, 16)
            && chunkZ >= Math.floorDiv(-(length / 2), 16) && chunkZ <= Math.floorDiv(length / 2 - 1, 16);
    }

    /** One line for a report, log or refusal message. */
    public String describe() {
        return this.type + "/" + this.theme + " " + width + "x" + length + "x" + height
            + (customized ? " (customized)" : " (shipped default)");
    }
}
