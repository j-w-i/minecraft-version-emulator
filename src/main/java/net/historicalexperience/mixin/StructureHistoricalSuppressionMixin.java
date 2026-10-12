package net.historicalexperience.mixin;

import java.util.function.Predicate;

import net.historicalexperience.generation.HistoricalFeatureExclusions;
import net.historicalexperience.generation.HistoricalStructureRules;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops a structure generating in a version that did not have it, at the moment of generation.
 *
 * <h2>The hook, and why this one</h2>
 *
 * <p>Every structure is built through {@code Structure.generate}, and the game already has a value for
 * "nothing here": {@code StructureStart.INVALID_START}, which this very method returns when no valid
 * generation point exists. Returning it means the structure is <b>never generated</b> - there is no
 * generate-then-remove step - and no caller can tell a suppressed structure from one that simply did not
 * fit, so nothing downstream has to know this hook exists.
 *
 * <h2>Backport difference, and a crash it caused</h2>
 *
 * <p>26.3's {@code generate} takes the structure as a {@code Holder} plus its {@code ResourceKey} and a
 * {@code Climate.Sampler}; 1.21.1's takes neither and passes no sampler. The first version of this file kept
 * 26.3's parameter list, which Mixin rejected as an invalid descriptor and which crashed the game during
 * bootstrap rather than merely failing to suppress anything. The signature below is 1.21.1's, taken from
 * the mapped jar rather than adjusted by eye.
 *
 * <p>Because no structure {@code Holder} arrives, identity comes from the caller's dynamic structure
 * registry via {@code registryAccess.registryOrThrow(Registries.STRUCTURE).getResourceKey(this)}.
 * There is no built-in structure-instance registry on 1.21.1.
 *
 * <h2>Scope</h2>
 *
 * <p>Only versions with a defended boundary are affected. Infdev, Alpha and Beta 1.7.3 generate no
 * structure-set structures because none existed; every later version is left alone until its structure
 * history is actually researched, which is recorded as a gap rather than filled in from memory.
 */
@Mixin(Structure.class)
public abstract class StructureHistoricalSuppressionMixin {
    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$suppressStructuresTheVersionDidNotHave(
        RegistryAccess registryAccess,
        ChunkGenerator chunkGenerator,
        BiomeSource biomeSource,
        RandomState randomState,
        StructureTemplateManager structureTemplateManager,
        long seed,
        ChunkPos sourceChunkPos,
        int references,
        LevelHeightAccessor heightAccessor,
        Predicate<Holder<Biome>> validBiome,
        CallbackInfoReturnable<StructureStart> callback
    ) {
        // Structures are data-driven in 1.21.1, so they are not in BuiltInRegistries at all; the id comes
        // from the dynamic registry the caller handed us. Looking it up the other way round - the registry
        // asked for the key of the instance it placed - avoids guessing which structure this is.
        var key = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
            .getResourceKey((Structure) (Object) this).orElse(null);
        if (key == null) {
            // An unregistered structure is not something this rule can have an opinion about.
            return;
        }
        String alias = HistoricalFeatureExclusions.activeAlias(chunkGenerator);
        if (HistoricalStructureRules.suppresses(alias, key.location().toString())) {
            callback.setReturnValue(StructureStart.INVALID_START);
        }
    }
}
