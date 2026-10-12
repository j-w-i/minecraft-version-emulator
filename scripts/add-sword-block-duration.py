"""Add upstream's use-duration injection to the sword-block mixin.

Upstream, combat_player/LivingEntityMixin lines 52-68: @ModifyExpressionValue on the getUseDuration call inside
startUsingItem, returning 72000 when a player can sword block. Without it a sword has no use duration, so the
stance starts and ends in the same tick and the block is never held - the damage-reduction hook would still fire,
but there would be nothing for it to reduce.

This is the piece I listed as "not ported" in this file's own header when the damage half went in, so the header
is corrected here too.

Composition note: LivingEntityHistoricalInstantEatMixin modifies the SAME expression, returning 1 for food. The
two conditions are mutually exclusive - a sword is not edible - so the order they chain in cannot change the
result, which is why they can stay in separate mixins that each own one rule.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/mixin/LivingEntityHistoricalSwordBlockingMixin.java')
text = path.read_text(encoding='utf-8')

old = ''' * <p>NOT ported here, and visible rather than implied: upstream's paired injection that lengthens the sword's use
 * duration so a block is not cancelled the instant it starts, and the client-side arm and item animations, which
 * are a separate carrier ({@code SwordBlockingRenderer} and the two render mixins).
 */'''
new = ''' * <p>The paired use-duration injection is now here as well. Upstream forces the duration to 72000 ticks for a
 * player who can block, because a sword has no use duration of its own - so without it the stance begins and ends
 * in the same tick and there is no block for this hook to reduce.
 *
 * <p>NOT ported, and visible rather than implied: the client-side arm and item animations, which are a separate
 * carrier ({@code SwordBlockingRenderer} plus the two render mixins), and upstream's
 * {@code BLOCK_WITH_SWORD_ON_SHIELD} case, which the renderer asks about.
 */'''
assert old in text, 'header anchor not found'
text = text.replace(old, new, 1)

old = '''@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalSwordBlockingMixin {
    @ModifyVariable(argsOnly = true, method = "hurt", at = @At("HEAD"))'''
new = '''@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalSwordBlockingMixin {
    /**
     * Upstream: {@code nt_combat_player$modifyGetUseDuration}. 72000 ticks is one hour, which in practice means
     * the stance lasts until the player lets go.
     */
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
        method = "startUsingItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getUseDuration("
                + "Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int historicalExperience$swordBlockUseDuration(int useDuration) {
        if ((Object) this instanceof Player player && HistoricalSwordBlocking.canBlock(player)) {
            return 72000;
        }
        return useDuration;
    }

    @ModifyVariable(argsOnly = true, method = "hurt", at = @At("HEAD"))'''
assert old in text, 'class anchor not found'
text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('added the use-duration injection')
