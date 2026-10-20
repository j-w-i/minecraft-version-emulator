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
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Not a test: a {@code main} that a test runs in a fresh JVM. It touches ONE class first, then prints a digest of
 * every table that is derived from the registry at class-initialisation time, so the same digest is expected
 * whichever class happens to be loaded first.
 */
public final class InitOrderProbe {
    private InitOrderProbe() {
    }

    public static void main(String[] args) throws Exception {
        for (String first : args[0].split(",")) {
            Class.forName(first);
        }
        TreeMap<String, Object> digest = new TreeMap<>();
        digest.put("versions", HistoricalVersions.all().stream().map(HistoricalVersion::id).toList());
        digest.put("aliases", new TreeSet<>(HistoricalVersions.allRevisionAliases()));
        digest.put("markers", new TreeSet<>(net.historicalexperience.HistoricalMarker.HISTORICAL_PRESET_IDS));
        digest.put("profiles", new TreeSet<>(net.historicalexperience.profile.HistoricalProfiles.all().keySet().stream()
            .map(id -> id.value()).toList()));
        TreeMap<String, List<String>> features = new TreeMap<>();
        for (Feature feature : Feature.values()) {
            features.put(feature.name(), HistoricalVersions.all().stream()
                .filter(version -> Features.active(feature, version))
                .map(HistoricalVersion::id).toList());
        }
        digest.put("features", features);
        System.out.println("DIGEST " + digest.hashCode() + " " + digest.toString().length());
    }
}