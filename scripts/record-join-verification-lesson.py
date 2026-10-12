"""Record the verification lesson from round 93-94: a server boot cannot see player-join failures."""

import pathlib

path = pathlib.Path(__file__).resolve().parents[1] / 'docs/nt-port-audit.md'
text = path.read_text(encoding='utf-8')

text += '''

## A server boot does NOT exercise player join (rounds 93-94)

The most expensive lesson of this port, recorded because it invalidated a whole category of evidence.

`FoodDataHistoricalRegenerationMixin` modified the second `Player.addExhaustion` call in `FoodData.tick` with
`ordinal = 1`. Only one such call exists in that method, so the injection never matched - and a failed `@ModifyArg`
is FATAL when the class is transformed. `FoodData` is transformed when a PLAYER IS PLACED IN THE WORLD, so every
join threw:

    Couldn't place player in world
    Mixin transformation of net.minecraft.world.food.FoodData failed
    InjectionError: Critical injection failure: Argument modifier method
        historicalExperience$noExhaustionFromRegeneration
    Client disconnected with reason: Invalid player data

The owner reported "invalid player data" twice and I spent three rounds on it, including moving their player data
aside - which was treating a symptom that did not exist. The files were never the problem.

WHY EVERY CHECK MISSED IT: this project's generation and gameplay probes run a dedicated server and never place a
player, so `FoodData` was never transformed. Every "boots clean" reported since that mixin went in was blind to
this entire class of failure - and it is the class that breaks the game for a player.

RULES THAT FOLLOW:

1. **A boot check validates only the classes the boot touches.** An injection on a class that is first transformed
   at player join, world load or dimension change is unverified by it. Say which classes a run exercised.
2. **`ordinal =` is a guess until the count is verified against the bytecode.** Two other failures this session
   came from the same habit, and the audit of all ten ordinal sites is now in this file.
3. **A client run that joins a world is the only check that exercises player join.** It was restored in round 94
   and a player joined with no injection failure, which is what verifies the six client-side ordinals - armour
   tint, HUD elements, sword-block pose, instant-eat animation - that no server run ever touched.
4. **Client harness worlds must be client-created.** Worlds copied from a server probe do not open in the client,
   which is what made the harness look broken for eight rounds.
5. **Stop the client gracefully.** The probe now asks the process to close and waits up to 30 seconds before
   forcing, because a force-kill during a save is what half-wrote player data twice.
'''

path.write_text(text, encoding='utf-8')
print('audit records the verification lesson:', len(text.splitlines()), 'lines')
