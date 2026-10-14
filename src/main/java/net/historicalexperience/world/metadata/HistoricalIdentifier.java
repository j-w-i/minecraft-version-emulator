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
package net.historicalexperience.world.metadata;

import java.util.Objects;

/**
 * A tiny namespace:path identifier validator and value type.
 *
 * <p>This deliberately does not depend on any Minecraft or Minecraft-adjacent type: persisted
 * metadata must be readable and testable without a running game, and the persistence layer must
 * not change meaning when the game's own identifier parsing changes.
 */
public final class HistoricalIdentifier implements Comparable<HistoricalIdentifier> {
    private static final String VALID_PATH_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz_-./";

    private final String namespace;
    private final String path;
    private final String value;

    private HistoricalIdentifier(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
        this.value = namespace + ":" + path;
    }

    /**
     * Parses {@code namespace:path}. Both halves are required; the namespace must be a valid
     * lowercase alphanumeric/underscore/dot/hyphen token and the path must match the character
     * set Minecraft accepts for resource paths.
     *
     * @throws IllegalArgumentException when the value is not a well formed identifier
     */
    public static HistoricalIdentifier parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("identifier must not be null");
        }
        int colon = raw.indexOf(':');
        if (colon <= 0 || colon != raw.lastIndexOf(':') || colon == raw.length() - 1) {
            throw new IllegalArgumentException(
                "identifier must be of the form 'namespace:path': '" + raw + "'");
        }
        String namespace = raw.substring(0, colon);
        String path = raw.substring(colon + 1);
        validateNamespace(namespace, raw);
        validatePath(path, raw);
        return new HistoricalIdentifier(namespace, path);
    }

    /** Returns {@code true} when {@link #parse(String)} would succeed. */
    public static boolean isValid(String raw) {
        try {
            parse(raw);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void validateNamespace(String namespace, String raw) {
        for (int i = 0; i < namespace.length(); i++) {
            char c = namespace.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.'
                || c == '-';
            if (!ok) {
                throw new IllegalArgumentException(
                    "illegal character '" + c + "' in identifier namespace of '" + raw + "'");
            }
        }
    }

    private static void validatePath(String path, String raw) {
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (VALID_PATH_CHARS.indexOf(c) < 0) {
                throw new IllegalArgumentException(
                    "illegal character '" + c + "' in identifier path of '" + raw + "'");
            }
        }
        if (path.startsWith("/") || path.endsWith("/") || path.contains("//")) {
            throw new IllegalArgumentException("illegal path segment in identifier '" + raw + "'");
        }
    }

    public String namespace() {
        return namespace;
    }

    public String path() {
        return path;
    }

    /** The canonical {@code namespace:path} form. */
    public String value() {
        return value;
    }

    @Override
    public int compareTo(HistoricalIdentifier other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof HistoricalIdentifier other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
