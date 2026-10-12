"""Add a quick-play client run config, so client mixins can actually be launched and verified.

There was no client run config at all. scripts/client-launch-probe.ps1 runs `gradlew runClientQuickPlay` and
stamps run/client/saves/<World>/level.dat, so the task name and the run directory both have to match it.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
path = root / 'build.gradle'
text = path.read_text(encoding='utf-8')

anchor = '        server {'
addition = '''        // A quick-play client, so CLIENT mixins can be verified the way server ones are. There was no client
        // run config at all, which is why scripts/client-launch-probe.ps1 failed with
        // "Task 'runClientQuickPlay' not found" - and why the HUD mixin from round 31 could not be checked.
        // Client code that cannot be launched cannot be verified, and unverified is the state this project
        // keeps having to dig itself out of.
        clientQuickPlay {
            client()
            configName = 'Client Quick Play'
            runDir 'run/client'
            programArgs '--quickPlaySingleplayer',
                (project.findProperty('quickPlayWorld') ?: 'hexflash')
        }
        server {'''
assert anchor in text, 'server run config anchor not found'
assert 'clientQuickPlay' not in text, 'already applied'
path.write_text(text.replace(anchor, addition, 1), encoding='utf-8')
print('added the clientQuickPlay run config')
