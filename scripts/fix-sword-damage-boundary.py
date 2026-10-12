"""Give the sword its pre-Beta-1.9-Prerelease-3 damage, which my own file admitted it was getting wrong.

HistoricalDamageValues has said since round 27: "correct for the axe, pickaxe, shovel and hoe in all of them, and
correct for the sword from Beta 1.9 Prerelease 3 onward" - i.e. knowingly wrong for swords in SIX of the ten
profiles, written down and then left. That is the same class of error the owner caught in the fall damage gate: a
sub-behaviour carrying its parent's boundary.

Boundary: Beta 1.9 Prerelease 3 reduced sword damage "to keep enchanting balanced", so before it every sword is
one point higher. Six declared profiles are on the earlier side: both Infdev builds, Alpha, Beta 1.7.3, Beta 1.8.1
(and Beta 1.9 Prerelease 3 itself is the proxy for the change, so it is on the later side).

Also exposed by the same research row, and NOT fixed here because it is a separate behaviour: the timeline records
"1.5 All attacks from players now deal 1 more damage than intended unless the player is currently moving upwards in
singleplayer" and "1.8 Pre-release Player attacks no longer deal 1 more damage than intended". That window covers
Release 1.6.4, so player melee there should be one point higher than the weapon table says. Recorded rather than
guessed at, since it is an attack-level effect, not an item value.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')
old = '''    public static boolean blockFallDamageReductionEnabled(String versionId) {'''
new = '''    /**
     * Whether a version is older than another declared version, exposed so a rule can state its own boundary.
     *
     * <p>The private comparator has been here all along; rules that need a two-sided or sub-behaviour boundary
     * could not reach it, which is how the fall damage reduction ended up wearing the blocking gate's window.
     */
    public static boolean olderThanVersion(String versionId, String boundaryVersionId) {
        return isDeclaredVersion(versionId) && olderThan(versionId, boundaryVersionId);
    }

    public static boolean blockFallDamageReductionEnabled(String versionId) {'''
assert old in text, 'anchor not found'
text = text.replace(old, new, 1)
tweaks.write_text(text, encoding='utf-8')
print('exposed olderThanVersion')

old = '''    public static boolean blockFallDamageReductionEnabled(String versionId) {'''
new = '''    /**
     * Whether a sword deals one point more than the table below says, which is every version before Beta 1.9
     * Prerelease 3.
     *
     * <p>Named rather than inlined because the boundary is the point: a reader should be able to ask "which
     * profiles have the higher sword damage?" without reading a comparison inside a damage calculation.
     */
    public static boolean swordDamageIsOneHigher(String versionId) {
        return isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_9_PRE_3);
    }

    public static boolean blockFallDamageReductionEnabled(String versionId) {'''
text = text.replace(old, new, 1)
tweaks.write_text(text, encoding='utf-8')
print('added swordDamageIsOneHigher')

damage = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalDamageValues.java'
text = damage.read_text(encoding='utf-8')

old = '''    private static float eraAttackDamage(TieredItem tieredItem) {
        return switch (tieredItem) {
            case SwordItem sword -> sword.getTier().getAttackDamageBonus() + 6.0F;
            case AxeItem axe -> axe.getTier().getAttackDamageBonus() + 2.0F;
            case PickaxeItem pickaxe -> pickaxe.getTier().getAttackDamageBonus() + 1.0F;
            case ShovelItem shovel -> shovel.getTier().getAttackDamageBonus() + 0.5F;
            default -> tieredItem.getTier().getAttackDamageBonus();
        };
    }'''
new = '''    private static float eraAttackDamage(TieredItem tieredItem) {
        float damage = switch (tieredItem) {
            case SwordItem sword -> sword.getTier().getAttackDamageBonus() + 6.0F;
            case AxeItem axe -> axe.getTier().getAttackDamageBonus() + 2.0F;
            case PickaxeItem pickaxe -> pickaxe.getTier().getAttackDamageBonus() + 1.0F;
            case ShovelItem shovel -> shovel.getTier().getAttackDamageBonus() + 0.5F;
            default -> tieredItem.getTier().getAttackDamageBonus();
        };

        // Beta 1.9 Prerelease 3 reduced sword damage "to keep enchanting balanced", so every sword is one point
        // higher before it. This file said so for twenty rounds while applying the later value to all ten profiles.
        if (tieredItem instanceof SwordItem
            && HistoricalGameplayTweaks.swordDamageIsOneHigher(HistoricalActiveSession.current())) {
            damage += 1.0F;
        }

        return damage;
    }'''
assert old in text, 'eraAttackDamage anchor not found'
text = text.replace(old, new, 1)
damage.write_text(text, encoding='utf-8')
print('swords now get their pre-PR3 value')
