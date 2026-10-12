"""Port the sword-block fall damage reduction, with the magnitude the wiki supplies.

Sourced magnitude (minecraft.wiki/w/Blocking, History, 1.0.0 / Beta 1.9 Prerelease):
    "Blocking no longer decreases fall damage. Blocking previously allowed the player to fall 42 blocks without
     dying, instead of 22."

Fall damage is linear in distance beyond the safe distance, so a lethal distance of 22 blocks becoming 42 is the
damage being HALVED - the same 50% the sword-blocking row cites for melee. Two independent statements agreeing is
what makes this a number rather than a guess, and it is why this half went in while the underwater-rise half did
not (the wiki confirms that mechanic and its removal but never gives a magnitude).

Hooking the MULTIPLIER argument of calculateFallDamage is deliberate: "blocking halves fall damage" then reads in
the code the way it reads in the source. The multiplier also happens to be the second float, so argsOnly with
ordinal = 1 is needed - by type alone it would be ambiguous, which is how a silent wrong-argument bug would start.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/mixin/LivingEntityHistoricalSwordBlockingMixin.java')
text = path.read_text(encoding='utf-8')

old = ''' * <p>The paired use-duration injection is now here as well.'''
new = ''' * <p>The fall damage half is here too, and it is the other documented sub-behaviour of the old stance: blocking
 * used to reduce fall damage as well as melee. Magnitude from the wiki's Blocking history, under 1.0.0 / Beta 1.9
 * Prerelease: "Blocking previously allowed the player to fall 42 blocks without dying, instead of 22." Fall damage
 * being linear in distance, 22 becoming 42 is the damage being halved - which agrees with the 50% the sword
 * blocking row already cites, and the code reuses that same multiplier so the two cannot drift apart. The wiki
 * confirms the companion mechanic, rising faster underwater, but never gives a magnitude for it, so that one is
 * not here.
 *
 * <p>The paired use-duration injection is now here as well.'''
assert old in text, 'header anchor not found'
text = text.replace(old, new, 1)

old = '''    @ModifyVariable(argsOnly = true, method = "hurt", at = @At("HEAD"))'''
new = '''    /**
     * Upstream has no equivalent: Nostalgic Tweaks does not implement the fall damage half. Implemented from the
     * research row and the wiki quote in this file's header, which is why the magnitude is cited rather than
     * chosen.
     *
     * <p>{@code ordinal = 1} selects the multiplier, not the distance, which by type alone would be ambiguous.
     */
    @ModifyVariable(method = "calculateFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float historicalExperience$reduceBlockedFallDamage(float multiplier) {
        if ((Object) this instanceof Player player && HistoricalSwordBlocking.isBlocking(player)) {
            return multiplier * HistoricalSwordBlocking.damageMultiplier();
        }
        return multiplier;
    }

    @ModifyVariable(argsOnly = true, method = "hurt", at = @At("HEAD"))'''
assert old in text, 'method anchor not found'
text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('added the fall damage reduction, citing the 42-vs-22 quote')
