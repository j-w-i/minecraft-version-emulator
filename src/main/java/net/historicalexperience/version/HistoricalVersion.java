/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.version;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * One historical Minecraft version, with every rule the mod applies to it.
 *
 * <p>This is the single declaration point the project was missing. Before it existed, adding a version
 * meant editing a marker list, a profile table, and a separate rule table per subsystem, each with its
 * own keying convention. Now a version declares its generation revisions and its rule set in one entry,
 * and every subsystem looks up through {@link HistoricalVersions}.
 *
 * <h2>The two axes, and which rules belong on which</h2>
 *
 * <ul>
 *   <li>{@link GenerationRevision}s are <b>immutable and append-only</b>, and they own
 *       <b>generation-content</b> rules: which placed features are excluded and whether the revision
 *       places its own ores. A revision is a persisted recipe identifier, so it can never be edited or
 *       re-pointed, and a new behaviour is a new revision. Putting these rules on the version instead
 *       would make every earlier revision of that version suddenly generate differently, which is the
 *       exact failure the revision scheme exists to prevent.</li>
 *   <li>{@link VersionRules} own <b>simulation</b> rules: natural spawning, mob drops, experience,
 *       sprinting and hunger. These change what happens next rather than what a stored chunk contains,
 *       so they are keyed by the version and apply to every revision of it - including worlds that
 *       already exist.</li>
 * </ul>
 *
 * @param id          the gameplay profile identifier, e.g. {@code historical_experience:beta_1_7_3}
 * @param displayName a human readable name, never null
 * @param era         the era family, used for reporting and for "which eras exist" queries
 * @param chronology  ordering key: a larger number is a later version
 * @param rules       the simulation rules this version changes
 * @param revisions   the immutable generation recipes belonging to this version, oldest first
 */
public record HistoricalVersion(
    String id,
    String displayName,
    String era,
    int chronology,
    VersionRules rules,
    List<GenerationRevision> revisions
) {
    public HistoricalVersion {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("version id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank for " + id);
        }
        if (era == null || era.isBlank()) {
            throw new IllegalArgumentException("era must not be blank for " + id);
        }
        if (rules == null) {
            throw new IllegalArgumentException("rules must not be null for " + id);
        }
        revisions = List.copyOf(revisions);
        if (revisions.isEmpty()) {
            throw new IllegalArgumentException(id + " declares no generation revision");
        }
        Set<String> aliases = new java.util.HashSet<>();
        for (GenerationRevision revision : revisions) {
            // The frozen legacy declaration deliberately uses the same string for its recipe alias and
            // its version id: that is what v1 persisted, and changing it would break every world already
            // created. A NUMBERED revision must not collide, because those exist to be distinguishable
            // from the original.
            if (revision.revision() > 0 && revision.alias().equals(id)) {
                throw new IllegalArgumentException(
                    id + " has a numbered revision whose alias equals the version id; a revision must "
                        + "be distinguishable from the original declaration");
            }
            if (!aliases.add(revision.alias())) {
                throw new IllegalArgumentException(
                    id + " declares the revision alias " + revision.alias() + " twice");
            }
        }
    }

    /** Whether this version is strictly earlier than a position on the timeline. */
    public boolean isBefore(Milestone boundary) {
        return this.chronology < boundary.key();
    }

    /** Whether this version is at or after a position on the timeline. */
    public boolean isAtOrAfter(Milestone boundary) {
        return this.chronology >= boundary.key();
    }

    /** Whether this version lies in {@code [from, until)}. */
    public boolean isWithin(Milestone from, Milestone until) {
        return isAtOrAfter(from) && isBefore(until);
    }

    /** The historical family this version belongs to, derived from its chronology key. */
    public Era family() {
        return Era.of(this.chronology);
    }

    /** The newest revision, which is the one a newly created world of this version uses. */
    public GenerationRevision latestRevision() {
        return revisions().get(revisions().size() - 1);
    }

    /**
     * Revisions that have been ACCEPTED, i.e. generated in a real world and probed, so `experimental` may be false.
     *
     * <p>Before this list existed the record's invariant said a numbered revision "must be marked experimental until it
     * is accepted", but there was no way to express acceptance at all - the flag could never legally become false. The
     * 256-height revisions exposed that: one of them has now been created and probed at runtime, and the only correct
     * value for its flag was the one the invariant forbade.
     *
     * <p>Deliberately a small, explicit, reviewed list rather than a judgement inside the constructor. An alias joins
     * it when a dedicated server has generated a world on it and the probe named in its evidence passed; the list is
     * what a reviewer checks, so accepting a revision is a visible act. All five 256-height revisions joined it after
     * `scripts/verify-release-height.ps1` reported 0..255 buildable and y=256/y=-1 out of world for each build.
     */
    public static final java.util.Set<String> ACCEPTED_REVISIONS = java.util.Set.of(
        "historical_experience:release_1_2_5_height_256",
        "historical_experience:release_1_3_height_256",
        "historical_experience:release_1_4_height_256",
        "historical_experience:release_1_5_height_256",
        "historical_experience:release_1_6_4_height_256");

    /** All owned recipe aliases of this version. */
    public Set<String> revisionAliases() {
        return revisions().stream().map(GenerationRevision::alias)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * One immutable generation recipe and the generation-content rules it freezes.
     *
     * @param revision               numbering within the version; 0 is the frozen original declaration
     * @param alias                  the project owned settings preset a world references and metadata persists
     * @param foundation             the actual upstream Moderner Beta preset the alias inherits terrain from
     * @param experimental           true for a numbered revision that is not an accepted historical profile
     * @param description            what this revision changes, for reports and reviews
     * @param excludedPlacedFeatures placed features this revision must not generate
     * @param orePopulation          ore veins this revision places itself; empty means vanilla placement
     */
    public record GenerationRevision(
        int revision,
        String alias,
        String foundation,
        boolean experimental,
        String description,
        List<Exclusion> excludedPlacedFeatures,
        List<OreRule> orePopulation
    ) {
        public GenerationRevision {
            if (revision < 0) {
                throw new IllegalArgumentException("revision must be >= 0, got " + revision);
            }
            if (alias == null || alias.isBlank()) {
                throw new IllegalArgumentException("alias must not be blank");
            }
            if (foundation == null || foundation.isBlank()) {
                throw new IllegalArgumentException("foundation must not be blank for " + alias);
            }
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException("description must not be blank for " + alias);
            }
            boolean legacy = revision == 0;
            // An unnumbered original is never experimental; a numbered revision is experimental UNLESS it is on the
            // reviewed accepted list, which is the only way the flag can be false without lying about its status.
            boolean wronglyFlagged = legacy ? experimental
                : (!experimental && !ACCEPTED_REVISIONS.contains(alias));
            if (wronglyFlagged) {
                throw new IllegalArgumentException(
                    "revision " + revision + " of " + alias + " declares experimental="
                        + experimental + "; the unnumbered original is not experimental and a numbered "
                        + "revision must be marked experimental until it is accepted");
            }
            excludedPlacedFeatures = List.copyOf(excludedPlacedFeatures);
            orePopulation = List.copyOf(orePopulation);
        }

        /** True for the frozen, unnumbered original declaration. */
        public boolean legacy() {
            return revision == 0;
        }

        /** True when this revision replaces vanilla ore placement with its own. */
        public boolean placesOwnOres() {
            return !orePopulation.isEmpty();
        }
    }

    /**
     * Everything a version changes about <b>simulation</b>. Empty collections mean "leave the modern
     * behaviour alone", which is what keeps ordinary worlds and unimplemented subsystems untouched by
     * default.
     *
     * @param restrictsSpawns      whether this version restricts natural spawning AT ALL. Separate from
     *                             the set, because the evidence separated them: Infdev 20100227
     *                             instantiates no mob of any kind, so its correct rule is "nothing
     *                             spawns" - an EMPTY allowed set that still restricts. Reading an empty
     *                             set as "unrestricted" would have let modern mobs spawn in the
     *                             least mob-populated version of the game
     * @param allowedNaturalSpawns entity types allowed to spawn naturally; with {@code restrictsSpawns}
     *                             true, an empty set means nothing may spawn naturally
     * @param mobDrops             death drops to substitute, keyed by entity type id
     * @param foodHealPoints       direct food healing, for versions with no hunger system
     * @param awardsExperience     whether mobs award experience at all
     * @param allowsSprinting      whether the player may sprint
     * @param hasHunger            whether the hunger system exists
     * @param hasStructures        whether the version generates structures
     * @param evidence             how far this rule set has been verified, in plain words
     */
    public record VersionRules(
        boolean restrictsSpawns,
        Set<String> allowedNaturalSpawns,
        Map<String, DropRule> mobDrops,
        Map<String, Integer> foodHealPoints,
        boolean awardsExperience,
        boolean allowsSprinting,
        boolean hasHunger,
        boolean hasStructures,
        String evidence
    ) {
        public VersionRules {
            allowedNaturalSpawns = Set.copyOf(allowedNaturalSpawns);
            mobDrops = Map.copyOf(mobDrops);
            foodHealPoints = Map.copyOf(foodHealPoints);
            for (Map.Entry<String, Integer> entry : foodHealPoints.entrySet()) {
                // A non-positive heal would be a silent no-op at runtime, which is the kind of value
                // error that is much cheaper to catch here than in play.
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    throw new IllegalArgumentException("food heal for " + entry.getKey()
                        + " must be positive, got " + entry.getValue());
                }
            }
            if (!restrictsSpawns && !allowedNaturalSpawns.isEmpty()) {
                throw new IllegalArgumentException(
                    "a rule set that does not restrict spawning must not name allowed spawns; the two "
                        + "would disagree about whether anything is filtered");
            }
            if (evidence == null || evidence.isBlank()) {
                throw new IllegalArgumentException("a rule set must state how it was verified");
            }
        }

        /** True when this version replaces any mob's death drops. */
        public boolean replacesLoot() {
            return !mobDrops.isEmpty();
        }
    }

    /** A rule set that changes nothing, for a version whose rules are not researched yet. */
    public static VersionRules unmodified(String evidence) {
        return new VersionRules(false, Set.of(), Map.of(), Map.of(), true, true, true, true, evidence);
    }
}
