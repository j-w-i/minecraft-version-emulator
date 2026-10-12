"""Remove the regeneration mixin's addExhaustion injection - it fails to match and breaks player join.

The client log named it exactly:

    Couldn't place player in world
    Mixin transformation of net.minecraft.world.food.FoodData failed
    InjectionError: Critical injection failure: Argument modifier method
        historicalExperience$noExhaustionFromRegeneration
    Client disconnected with reason: Invalid player data

So the "invalid player data" the owner saw twice had nothing to do with player data files - it is the generic
client message for the server failing to place a player, and the cause is this injection. It was written with
`ordinal = 1` on the `Player.addExhaustion` call inside FoodData.tick, from a bytecode reading where only ONE such
call was visible; assuming a second existed was the guess.

Two lessons, both already paid for once this session:
  * an ordinal is a guess unless the count is verified - the sweep argument and a WrapWithCondition arity both
    came from the same habit;
  * a server boot does NOT exercise player join. Every green boot check reported since this mixin went in was
    blind to this failure, because FoodData is only transformed when a player is placed.

The constant hook stays: skipping the fast regeneration branch is the main behavioural fix and it does not depend
on a guessed ordinal. The exhaustion half is recorded as not implemented rather than half-implemented.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/mixin/FoodDataHistoricalRegenerationMixin.java')
text = path.read_text(encoding='utf-8')

start = text.index('    /**\n     * Upstream has no equivalent. Zeroes the exhaustion charge')
end = text.index('    /** Whether the active version regenerates the Beta 1.8 way. */')
replacement = '''    // The exhaustion half of this tweak is NOT implemented. Its first version modified the second
    // Player.addExhaustion call in tick with ordinal = 1, but only one such call was visible in the bytecode, so
    // the injection failed to match - and a failed @ModifyArg is FATAL when the class is transformed, which is
    // when a player joins the world. That is what produced the owner's "invalid player data" twice.
    //
    // Recording it here rather than re-adding a guessed ordinal: research records regenerationCostsExhaustion =
    // False, so the charge should go, but which call site and whether there are two are things to verify against
    // the bytecode first.

'''
path.write_text(text[:start] + replacement + text[end:], encoding='utf-8')
print('removed the failing @ModifyArg; the constant hook remains')
