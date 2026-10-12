"""Restore the regeneration exhaustion suppression, targeting the right OWNER class this time.

The first version targeted `Lnet/minecraft/world/entity/player/Player;addExhaustion(F)V` with ordinal = 1. javap on
FoodData.tick shows the calls as:

    169: invokevirtual #127   // Method addExhaustion:(F)V
    227: invokevirtual #127   // Method addExhaustion:(F)V

with no class prefix, which means the owner in the constant pool is the CURRENT class: FoodData declares its own
addExhaustion, and Player.addExhaustion delegates to it. So the target never matched, and a failed @ModifyArg is
fatal when FoodData is transformed - which happens when a player is placed in the world. That is what produced the
owner's "invalid player data" twice.

Counting before choosing, as the failure taught:
  * addExhaustion appears TWICE in tick - occurrence 1 at 169 (fast branch, foodLevel >= 20, timer >= 10) and
    occurrence 2 at 227 (slow branch, foodLevel >= 18, timer >= 80). The fast branch is already made unreachable
    by the constant hook, so ordinal = 1 is the slow branch's charge, which is the one the research says should
    not be charged (regenerationCostsExhaustion = False).

VERIFIED: the owner class, the call count and which branch each call sits in, all from the bytecode.
NOT VERIFIED: that Mixin resolves the corrected target. Only a player join transforms FoodData, so the server
probe cannot check it - that is exactly the blind spot this file's own history is about.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/mixin/FoodDataHistoricalRegenerationMixin.java')
text = path.read_text(encoding='utf-8')

start = text.index('    // The exhaustion half of this tweak is NOT implemented.')
end = text.index('    /** Whether the active version regenerates the Beta 1.8 way. */')

replacement = '''    /**
     * Zeroes the regeneration exhaustion charge on the SLOW branch.
     *
     * <p>The owner class is {@code FoodData}, not {@code Player}: the calls inside {@code tick} resolve to
     * FoodData's own {@code addExhaustion}, and targeting {@code Player.addExhaustion} never matched - a fatal
     * error at transformation, which happens at player join. The ordinal is the second call in the method (offset
     * 227, the slow branch); the first (offset 169) belongs to the fast branch, which the constant hook above
     * already makes unreachable.
     */
    @ModifyArg(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V",
            ordinal = 1),
        index = 0)
    private float historicalExperience$noExhaustionFromRegeneration(float exhaustion) {
        return usesBetaRegeneration() ? 0.0F : exhaustion;
    }

'''
path.write_text(text[:start] + replacement + text[end:], encoding='utf-8')
print('restored the exhaustion suppression with the verified owner class')
