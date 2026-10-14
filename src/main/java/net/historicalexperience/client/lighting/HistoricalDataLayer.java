package net.historicalexperience.client.lighting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;

/**
 * A chunk section's light array, answering with the value the OLD engine would have stored.
 *
 * <p>Ported from Nostalgic Tweaks {@code helper/candy/light/NostalgicDataLayer} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 *
 * <h2>Why this exists alongside the {@code LightEngine.getLightValue} hook</h2>
 *
 * <p>The vanilla renderer asks the light engine for a value per block face, so hooking {@code getLightValue}
 * reaches it. <b>Sodium does not ask.</b> It copies each section's whole {@link DataLayer} up front
 * ({@code ClonedChunkSection.copyLightArray} -> {@code LayerLightEventListener.getDataLayerData}) and reads cells out
 * of its copy with {@code DataLayer.get(x, y, z)}. Verified against the Sodium 0.8.13 bytecode, not assumed. A hook on
 * {@code getLightValue} alone therefore changes nothing on screen for anyone running Sodium - which is most players,
 * and was the owner's instance. Returning this wrapper from {@code getDataLayerData} puts the same transformation in
 * front of both renderers.
 *
 * <p>The transformation is evaluated at READ time rather than baked in, so a section that is re-meshed after the time
 * of day has moved picks up the new value with no change to the stored data.
 */
public final class HistoricalDataLayer extends DataLayer {
    private final DataLayer parent;
    private final LightLayer layer;
    private final long sectionPos;

    public HistoricalDataLayer(DataLayer parent, LightLayer layer, long sectionPos) {
        this.parent = parent;
        this.layer = layer;
        this.sectionPos = sectionPos;
    }

    @Override
    public DataLayer copy() {
        return new HistoricalDataLayer(this.parent.copy(), this.layer, this.sectionPos);
    }

    @Override
    public int get(int x, int y, int z) {
        int value = this.parent.get(x, y, z);
        if (HistoricalOldLighting.mode() == HistoricalOldLighting.Mode.NONE) {
            return value;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return value;
        }
        BlockPos pos = SectionPos.of(this.sectionPos).origin().offset(x, y, z);
        return HistoricalOldLighting.transform(level, this.layer, pos, value);
    }

    // Everything that is not a read passes straight through, so the engine's own bookkeeping is untouched.

    @Override
    public void set(int x, int y, int z, int value) {
        this.parent.set(x, y, z, value);
    }

    @Override
    public boolean isEmpty() {
        return this.parent.isEmpty();
    }

    @Override
    public void fill(int defaultValue) {
        this.parent.fill(defaultValue);
    }

    @Override
    public byte[] getData() {
        return this.parent.getData();
    }

    @Override
    public boolean isDefinitelyHomogenous() {
        return this.parent.isDefinitelyHomogenous();
    }

    @Override
    public boolean isDefinitelyFilledWith(int value) {
        return this.parent.isDefinitelyFilledWith(value);
    }
}
