# Historical village residents

- Residents spawn at interior positions in houses, **not in wells, roads or farms**. Small houses and shacks receive one each; the larger upstream-populated buildings retain their one/two resident positions.
- Placement is clipped to the current generation chunk. Each piece saves a resident claim mask, so repeated callbacks or loading the saved piece cannot multiply its original residents.
- Beta1.8.1 villages intentionally remain empty; villagers start at Beta1.9 Prerelease1. Existing profession/plain-skin gates remain unchanged. Ordinary worlds do not receive this historical population hook.
- **Restart with the rebuilt mod and test newly generated villages.** This fixes future generation; it does not automatically repopulate already-generated empty villages, remove existing villagers or relocate existing well crowds. No terrain, block layout, loot table or frozen generation settings changed.

## Evidence and limits

[Native acceptance](<../run/client/village-resident-probe-1792166227822.json>) passes: a real Release1.1 generated village contains14 live loaded residents; the corresponding Beta1.8.1 village contains0. Separate callback fixtures test all nine residential types/four rotations (44 residents), a chunk-split two-resident house, no nonresidential spawns, real registered-piece compressed disk serialization/reload, three repeat callback passes with unchanged entity UUID sets, and exact existing villager data preservation. Release1.4 and ordinary-world controls pass too. This is developer Fabric1.21.1/Java21 evidence, not an original-artifact or full villager-AI certification.

Five building types use the pinned, already incorporated OldVillages spawn coordinates. Its missing small-house/shack calls caused layouts made only of those homes to have no residents. This repair explicitly adds safe interior positions inspected against their existing floor/wall/headroom geometry; it does not claim those additions are original upstream call sites or exact historical population algorithms. [Research](<../research/village-resident-placement.json>) and [provenance](<PROVENANCE.md>) record that distinction.

Reproduce with Java25 for Gradle and the pinned Java21 game toolchain:

```powershell
.\gradlew.bat --console=plain --offline build
.\gradlew.bat --console=plain --offline runClientQuickPlay -PtitleStart -PvillageResidentsProbe
```

The opt-in driver creates only new project-owned worlds under `run/client`, checks its pinned root, and confirms only Minecraft's specifically identified experimental-worldgen stability warning for those fixtures—not legal/EULA dialogs. A process exit0 alone is not acceptance: inspect the retained JSON's `passed` field. No personal saves/accounts/settings, terms acceptance, deployments or commits are involved.
