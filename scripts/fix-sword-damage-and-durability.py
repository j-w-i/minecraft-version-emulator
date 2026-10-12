"""Two corrections from research/historical-features.json, entry beta18-unarmed-and-sword-damage (high confidence).

1. THE SWORD OFFSET IS +3, NOT +1. The entry: "Release 1.0.0 | Sword damage decreased (diamond sword from 5
   hearts to 3.5 hearts)." Five hearts to three and a half is ten damage to seven - a difference of THREE, not the
   one I applied in round 72 from an unrelated later line ("1.9 15w34c: All swords now deal 1 less damage"). The
   measurement in round 72 already showed +3 and I explained it away as NT's table being too high; the research
   says NT's table is the later value, and the five profiles before Release 1.0.0 take the earlier one.

   Affected: infdev_420, infdev_611, alpha, beta_1_7_3, beta_1_8_1.

2. BLOCKING COSTS NO DURABILITY. The entry beta18-sword-blocking records swordBlockDurabilityLoss = False, and its
   change list says the sword "does not lose durability while blocking". Nostalgic Tweaks implements the opposite -
   its hook spends a point of durability per blocked hit - and I ported that faithfully because the instruction was
   to copy upstream. Where the two disagree about a version fact, the research decides, so the durability cost
   comes out. It is the one place so far where copying upstream was the wrong instruction.

Recorded but NOT implemented, because they are separate boundaries with their own versions and I will not guess
their shape: unarmed damage 2 in Beta 1.5-1.7 with the +1 floating-point bug on all player attacks (Beta 1.7.3 is
inside that window), and Release 1.6.2's "tools add their damage to the bare-hand 1 HP, raising all tool damage by
1" (Release 1.6.4 is the only profile at or after it).
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')
old = '''    public static boolean swordDamageIsOneHigher(String versionId) {
        return isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_9_PRE_3);
    }'''
new = '''    public static boolean swordDamageBeforeReleaseOnePointZero(String versionId) {
        return isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_9_PRE_3);
    }'''
assert old in text, 'predicate anchor not found'
text = text.replace(old, new, 1)
text = text.replace('''     * Whether a sword deals one point more than the table below says, which is every version before Beta 1.9
     * Prerelease 3.''',
'''     * Whether a sword deals the PRE-Release-1.0.0 value, which is every version before Beta 1.9 Prerelease 3.
     *
     * <p>The offset is three, not one: research/historical-features.json records "Sword damage decreased (diamond
     * sword from 5 hearts to 3.5 hearts)" at Release 1.0.0, and ten damage becoming seven is a difference of three.
     * An earlier version of this applied one, taken from a later and unrelated wiki line about 1.9.''', 1)
tweaks.write_text(text, encoding='utf-8')
print('renamed the predicate and corrected its documentation')

damage = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalDamageValues.java'
text = damage.read_text(encoding='utf-8')
old = '''            && HistoricalGameplayTweaks.swordDamageIsOneHigher(HistoricalActiveSession.current())) {
            damage += 1.0F;
        }'''
new = '''            && HistoricalGameplayTweaks.swordDamageBeforeReleaseOnePointZero(
                HistoricalActiveSession.current())) {
            // Three points, from ten damage to seven: "diamond sword from 5 hearts to 3.5 hearts" at Release 1.0.0.
            damage += 3.0F;
        }'''
assert old in text, 'sword offset anchor not found'
text = text.replace(old, new, 1)
damage.write_text(text, encoding='utf-8')
print('sword offset corrected to +3')

blocking = root / 'src/main/java/net/historicalexperience/mixin/LivingEntityHistoricalSwordBlockingMixin.java'
text = blocking.read_text(encoding='utf-8')
old = '''        if (hurtAmount > 1.0F) {
            player.getUseItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

            if (player.getUseItem().isEmpty()) {
                player.stopUsingItem();
            }
        }

        return hurtAmount * HistoricalSwordBlocking.damageMultiplier();'''
new = '''        // NO durability cost. Nostalgic Tweaks spends a point per blocked hit and this port copied that, but
        // research/historical-features.json records swordBlockDurabilityLoss = False for the Beta-era stance: the
        // sword "does not lose durability while blocking". The research decides version facts, so upstream's
        // behaviour is not followed here.
        return hurtAmount * HistoricalSwordBlocking.damageMultiplier();'''
assert old in text, 'durability anchor not found'
text = text.replace(old, new, 1)
blocking.write_text(text, encoding='utf-8')
print('removed the durability cost, with the research citation')
