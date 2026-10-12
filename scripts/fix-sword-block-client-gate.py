"""Fix the sword block on the client: ask the session, not the level.

The reported symptom: the block animation does not play. The cause is the same one that broke sprinting, in a
rule set that never got the fix.

HistoricalSwordBlocking.canBlock -> HistoricalGameplayTweaks.swordBlockingEnabled(player.level()) ->
versionOf(level), which only answers for a ServerLevel. On the client the player's level is a ClientLevel, so
versionOf returns null, canBlock is false, isBlocking is false, and BOTH client-side halves switch off: the
right-click handler never calls startUsingItem, and the model mixin never applies the arm pose. The server would
have reduced the damage of a block the player was never shown and possibly never entered.

Fix: a version-id overload of swordBlockingEnabled, which the Level overload delegates to, so there is still one
boundary; and canBlock asks the active session, which is set on both sides.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')
old = '''    public static boolean swordBlockingEnabled(Level level) {
        String versionId = versionOf(level);
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId);
    }'''
new = '''    public static boolean swordBlockingEnabled(Level level) {
        return swordBlockingEnabled(versionOf(level));
    }

    /**
     * The same boundary, asked about a version instead of a level.
     *
     * <p>Needed because the answer has to hold on the CLIENT too: a client's player level is a ClientLevel, so a
     * level-based question returns null there and the rule switches itself off on the side that draws the block.
     * {@code HistoricalActiveSession} is set on both sides, which is what client-visible rules must ask.
     */
    public static boolean swordBlockingEnabled(String versionId) {
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId);
    }'''
assert old in text, 'swordBlockingEnabled anchor not found'
tweaks.write_text(text.replace(old, new, 1), encoding='utf-8')
print('added the version-id overload')

blocking = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalSwordBlocking.java'
text = blocking.read_text(encoding='utf-8')
old = '''        return HistoricalGameplayTweaks.swordBlockingEnabled(player.level())
            && player.getMainHandItem().is(ItemTags.SWORDS);'''
new = '''        // The ACTIVE SESSION, not the player's level: this has to answer the same way on the client, where the
        // level is a ClientLevel and a level-based question is always false. Asking the level here is what made
        // the stance invisible while the server still reduced its damage.
        return HistoricalGameplayTweaks.swordBlockingEnabled(HistoricalActiveSession.current())
            && player.getMainHandItem().is(ItemTags.SWORDS);'''
assert old in text, 'canBlock anchor not found'
blocking.write_text(text.replace(old, new, 1), encoding='utf-8')
print('canBlock now asks the session')
