"""Extend the particle mixin to the 1.9-era combat particles, and NOT to the crit particle.

NT's CandyTweak has four default-true flags in CandyGroup.PARTICLE_ATTACK:

    OLD_SWEEP_PARTICLES         true   -> sweep particle           (ported)
    OLD_NO_DAMAGE_PARTICLES     true   -> DAMAGE_INDICATOR         (1.9+, absent from every declared version)
    OLD_NO_CRIT_PARTICLES       true   -> CRIT                     (NOT ported, see below)
    OLD_NO_MAGIC_HIT_PARTICLES  true   -> ENCHANTED_HIT            (1.9+, absent from every declared version)

The crit one is the trap. "Remove the crit particle" is a Nostalgic Tweaks user preference, not a version fact:
critical hits ARRIVE in the Beta 1.8 Pre-release, and so does the crit particle - it is the classic star burst
that 1.8 players remember. This project declares six versions from Beta 1.8.1 onward, all of which had it, so
porting that flag as a version rule would delete a piece of the era being emulated. It is left out until the
research says otherwise, and the reason is written where the next reader looks.

The other two are safe on the same reasoning that made the sweep particle safe: the damage indicator and the
enchanted-hit particle are 1.9 additions, later than the newest declared profile, so no version this project
emulates could draw them.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/mixin/ParticleEngineHistoricalSweepMixin.java')
text = path.read_text(encoding='utf-8')

old = '''        if (particle != null
            && options.getType() == ParticleTypes.SWEEP_ATTACK
            && HistoricalGameplayTweaks.appliesTo("sweep", HistoricalActiveSession.current())) {
            return null;
        }
        return particle;'''
new = '''        if (particle == null) {
            return null;
        }

        if (!HistoricalGameplayTweaks.appliesTo("sweep", HistoricalActiveSession.current())) {
            return particle;
        }

        // The gate above is the 1.9-combat boundary, which no declared version is on the far side of. Range 1.9
        // additions only: the sweep particle, the damage indicator and the enchanted-hit sparkle. CRIT is
        // deliberately absent - critical hits and their star burst arrive together in the Beta 1.8
        // Pre-release, and six declared profiles had them - so removing it would delete part of the era rather
        // than emulate it. Upstream has a flag for it, and a flag is not evidence.
        if (options.getType() == ParticleTypes.SWEEP_ATTACK
            || options.getType() == ParticleTypes.DAMAGE_INDICATOR
            || options.getType() == ParticleTypes.ENCHANTED_HIT) {
            return null;
        }

        return particle;'''
assert old in text, 'handler anchor not found'
path.write_text(text.replace(old, new, 1), encoding='utf-8')
print('extended to the 1.9-era combat particles, crit excluded with its reason')
