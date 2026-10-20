"""Make a client run prove the client's own state, in its log.

Every client-side failure this session was invisible: sprinting and sword blocking answered true on the server and
false on the client, and the client's session being unset would make every client rule inert while `/hex status`
looked healthy. A client run that joins a world is the only thing that exercises those paths, so it should leave
evidence rather than requiring someone to read a screen.

This logs, once per join, the client's session and the answers to the client-visible rules - in the same shape as
the server's /hex status, but computed on THIS side.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

path = root / 'src/main/java/net/historicalexperience/client/HistoricalExperienceClient.java'
text = path.read_text(encoding='utf-8')

old = '''                HistoricalClientRules.applySyncedVersion(payload.versionId());'''
new = '''                HistoricalClientRules.applySyncedVersion(payload.versionId());
                reportClientState("the server pushed " + payload.versionId());'''
assert old in text, 'payload anchor not found'
text = text.replace(old, new, 1)

old = '''    }
}'''
new = '''    }

    /**
     * Logs what THIS client believes, so a client run leaves evidence rather than requiring a screen.
     *
     * <p>Written because the failure mode that cost the most was silent: a rule answering true on the server and
     * false on the client, because a client's player level is a ClientLevel and any level-based question returns
     * null there. The session line is the decisive one - if it reads NONE, every client-side rule is inert no
     * matter how correct its mixin is.
     */
    private static void reportClientState(String cause) {
        String session = net.historicalexperience.version.ActiveVersion.current();

        LOGGER.info("[hex] client state after {}: session={} hungerBarHidden={} experienceBarHidden={}",
            cause,
            session == null ? "<NONE - client-side rules are inert>" : session,
            net.historicalexperience.client.hud.HudHelper.hideHungerBar(),
            net.historicalexperience.client.hud.HudHelper.hideExperienceBar());
    }
}'''
assert old in text, 'client class tail anchor not found'
path.write_text(text.replace(old, new, 1), encoding='utf-8')
print('client state is now logged on join')
