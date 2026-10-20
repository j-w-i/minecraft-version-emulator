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

/**
 * One excluded placed feature and the evidence for excluding it.
 *
 * <p>A pure data carrier owned by the version model: a {@link HistoricalVersion.GenerationRevision} lists its
 * exclusions, so the type cannot live in the generation package without making the version core depend on it.
 * It moved here unchanged from {@code HistoricalFeatureExclusions.Exclusion}; the lists of exclusions and the code
 * that applies them are still in {@code net.historicalexperience.generation}.
 *
 * @param featureId the placed feature registry identifier
 * @param reason    why the version cannot have it, and which call site adds it
 */
public record Exclusion(String featureId, String reason) {
}
