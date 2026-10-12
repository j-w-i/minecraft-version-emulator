"""Make the version-gated state visible from /hex status, so a report can be conclusive.

Damage values, the attack cooldown and the sweep attack all gate on HistoricalActiveSession. If that is unset
they are silently inert, and "it did not work" cannot be told apart from "the rule is not active here". This
prints the session and the gate answers where a player can see them.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
service = root / 'src/main/java/net/historicalexperience/server/HistoricalUpgradeService.java'
text = service.read_text(encoding='utf-8')

anchor = '''        return new Report(true, "This world is " + metadata.profileId(), details);'''
addition = '''        // What the version-gated behaviour is actually keyed on. Several rules - old damage values, the 1.9
        // cooldown and sweep, sprinting beyond the server - read the active session rather than a level, so a
        // session that was never set makes them silently inert. Reporting it turns "the fix did not work" into
        // an answerable question.
        String session = net.historicalexperience.gameplay.HistoricalActiveSession.current();
        details.add("active session: " + (session == null
            ? "<NONE - session-gated behaviour is inert in this process>"
            : session));
        if (session != null) {
            details.add("combat gates for this session: cooldownSuppressed="
                + net.historicalexperience.gameplay.HistoricalGameplayTweaks.appliesTo("cooldown", session)
                + " sweepSuppressed="
                + net.historicalexperience.gameplay.HistoricalGameplayTweaks.appliesTo("sweep", session)
                + " oldDamageValues="
                + net.historicalexperience.gameplay.HistoricalActiveSession.isHistorical());
        }
        return new Report(true, "This world is " + metadata.profileId(), details);'''
assert anchor in text, 'anchor not found'
service.write_text(text.replace(anchor, addition, 1), encoding='utf-8')
print('status now reports the active session and the combat gates')
