package net.historicalexperience.mixin;

import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Path;
import net.historicalexperience.HistoricalExperienceMod;
import net.historicalexperience.world.upgrade.GenerationUpgrade;
import net.historicalexperience.world.upgrade.UpgradeJournal;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Recover under the acquired session lock, BEFORE level.dat is decoded into loaded generators. */
@Mixin(LevelStorageSource.LevelStorageAccess.class)
public abstract class LevelStorageHistoricalUpgradeRecoveryMixin {
    @Shadow public abstract Path getLevelPath(LevelResource resource);
    @Shadow private void checkLock() { throw new AssertionError(); }

    @Inject(method = "getDataTag(Z)Lcom/mojang/serialization/Dynamic;", at = @At("HEAD"))
    private void historicalExperience$recoverBeforeDecode(boolean fallback,
            CallbackInfoReturnable<Dynamic<?>> callback) throws IOException {
        Path world = getLevelPath(LevelResource.ROOT);
        if (!UpgradeJournal.existsIn(world)) return;
        // Do not bypass vanilla's ownership check: even reading an interrupted journal cannot confer
        // permission to change a world which this LevelStorageAccess no longer owns.
        checkLock();
        try {
            GenerationUpgrade.recover(world);
            var advance = GenerationUpgrade.applyPendingAdvance(world);
            HistoricalExperienceMod.LOGGER.info("Recovered historical upgrade before world-data decode: {} {}",
                world, advance);
        } catch (Exception failure) {
            // No fallback to a guessed old/new identity. Keep the journal and abort loading.
            throw new IOException("Historical upgrade recovery refused before world-data decode in " + world
                + "; retain the journal and resolve the backup/metadata error before retrying", failure);
        }
    }
}
