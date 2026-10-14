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
package net.historicalexperience.client.textures;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides which dependency, if any, a pack is, from its metadata and contents. File names are never consulted.
 *
 * <h2>What identifies each pack (taken from the packs as published, not from their file names)</h2>
 *
 * <ul>
 *   <li><b>Golden Days Alpha</b>: provides the {@code golden_days_alpha} namespace. Its own {@code usage.txt} says it
 *       "is meant to be put over Programmer Art and Golden Days Base", i.e. it is an additive pack, not a standalone
 *       one.</li>
 *   <li><b>Golden Days Base</b>: provides the {@code golden_days} namespace (and not {@code golden_days_alpha}) and its
 *       {@code pack.mcmeta} description names "Golden Days".</li>
 *   <li><b>Programmer Art Continuation</b>: its {@code credits.txt} or {@code changelog.txt} opens with "Programmer
 *       Art Continuation Project". Its {@code pack.mcmeta} description is only a version string, so metadata alone
 *       cannot identify it. The project ships Base, Beta-Fied and Alpha-Fied variants; only Base is the layer this
 *       system wants, so the others are reported as the wrong variant rather than accepted.</li>
 * </ul>
 *
 * <p>An unrelated ZIP that merely has a familiar name has none of these, and is not a dependency. A pack that matches
 * by content but whose declared format range excludes the running game is reported as unsupported, not as found.
 */
public final class PackClassifier {
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final Pattern PACP_BASE_VERSION = Pattern.compile("PACP\\s+Base\\s+Pack\\s+([0-9][0-9A-Za-z._-]*)");
    private static final int TEXT_LIMIT = 16 * 1024;

    public enum Status {
        /** Matches by content and is usable. */
        OK,
        /** Not a resource pack at all (no readable root {@code pack.mcmeta}) or not one of the dependencies. */
        NOT_A_DEPENDENCY,
        /** The right project, but a variant this system does not layer. */
        WRONG_VARIANT,
        /** The right pack, but its declared formats exclude this game version. */
        UNSUPPORTED_FORMAT
    }

    /**
     * @param source   the dependency this pack is, or null when it is none of them
     * @param status   whether it is usable
     * @param version  the pack's own version string when it declares one, else empty
     * @param evidence what was matched, for display and logs
     */
    public record Classification(TextureSource source, Status status, String version, String evidence) {
        static Classification none(String evidence) {
            return new Classification(null, Status.NOT_A_DEPENDENCY, "", evidence);
        }
    }

    private PackClassifier() {
    }

    public static Classification classify(PackFiles files, int gamePackFormat) {
        Optional<String> mcmeta = files.readText("pack.mcmeta", TEXT_LIMIT);
        if (mcmeta.isEmpty()) {
            return Classification.none("no pack.mcmeta at the pack root");
        }
        JsonObject pack;
        try {
            JsonElement root = JsonParser.parseString(mcmeta.get());
            pack = root.isJsonObject() && root.getAsJsonObject().has("pack")
                ? root.getAsJsonObject().getAsJsonObject("pack") : null;
        } catch (RuntimeException unreadable) {
            return Classification.none("pack.mcmeta is not valid JSON");
        }
        if (pack == null) {
            return Classification.none("pack.mcmeta has no pack section");
        }
        String description = description(pack).toLowerCase(Locale.ROOT);
        Set<String> namespaces = files.namespaces();

        TextureSource source = null;
        String version = "";
        String evidence = "";
        Status status = Status.OK;

        if (namespaces.contains("golden_days_alpha")) {
            source = TextureSource.GOLDEN_DAYS_ALPHA;
            evidence = "provides the golden_days_alpha namespace";
        } else if (namespaces.contains("golden_days") && description.contains("golden days")) {
            source = TextureSource.GOLDEN_DAYS_BASE;
            evidence = "provides the golden_days namespace; description names Golden Days";
        } else {
            String credits = files.readText("credits.txt", TEXT_LIMIT).orElse("");
            String changelog = files.readText("changelog.txt", TEXT_LIMIT).orElse("");
            String marker = "programmer art continuation";
            if (credits.toLowerCase(Locale.ROOT).contains(marker)
                || changelog.toLowerCase(Locale.ROOT).contains(marker)) {
                source = TextureSource.CONTINUATION;
                evidence = "credits/changelog name the Programmer Art Continuation Project";
                String heading = firstLines(changelog, 4);
                Matcher base = PACP_BASE_VERSION.matcher(heading);
                if (base.find()) {
                    version = base.group(1);
                } else if (heading.toLowerCase(Locale.ROOT).contains("beta-fied")
                    || heading.toLowerCase(Locale.ROOT).contains("alpha-fied")) {
                    status = Status.WRONG_VARIANT;
                    evidence += "; the changelog heading is a Beta-Fied/Alpha-Fied variant, not the Base pack";
                } else {
                    evidence += "; Base variant not confirmed from the changelog heading";
                }
            }
        }
        if (source == null) {
            return Classification.none("not Golden Days or Programmer Art Continuation");
        }
        if (status == Status.OK && !supports(pack, gamePackFormat)) {
            status = Status.UNSUPPORTED_FORMAT;
            evidence += "; declared formats exclude " + gamePackFormat;
        }
        return new Classification(source, status, version, evidence);
    }

    /** The pack description as plain text, with legacy formatting codes removed. */
    static String description(JsonObject pack) {
        JsonElement element = pack.get("description");
        if (element == null) {
            return "";
        }
        return FORMATTING.matcher(flatten(element)).replaceAll("");
    }

    private static String flatten(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        StringBuilder out = new StringBuilder();
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(part -> out.append(flatten(part)));
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("text")) {
                out.append(flatten(object.get("text")));
            }
            if (object.has("extra")) {
                out.append(flatten(object.get("extra")));
            }
        }
        return out.toString();
    }

    private static String firstLines(String text, int count) {
        String[] lines = text.split("\\R", count + 1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(count, lines.length); i++) {
            out.append(lines[i]).append('\n');
        }
        return out.toString();
    }

    /** Whether the declared formats include {@code format}; a pack that declares none is taken as compatible. */
    static boolean supports(JsonObject pack, int format) {
        Integer min = null;
        Integer max = null;
        JsonElement supported = pack.get("supported_formats");
        if (supported != null) {
            if (supported.isJsonArray()) {
                JsonArray array = supported.getAsJsonArray();
                if (array.size() == 2) {
                    min = array.get(0).getAsInt();
                    max = array.get(1).getAsInt();
                }
            } else if (supported.isJsonObject()) {
                JsonObject object = supported.getAsJsonObject();
                if (object.has("min_inclusive") && object.has("max_inclusive")) {
                    min = object.get("min_inclusive").getAsInt();
                    max = object.get("max_inclusive").getAsInt();
                }
            } else if (supported.isJsonPrimitive()) {
                min = max = supported.getAsInt();
            }
        }
        if (min == null && pack.has("min_format") && pack.has("max_format")) {
            min = scalar(pack.get("min_format"));
            max = scalar(pack.get("max_format"));
        }
        if (min == null && pack.has("pack_format")) {
            min = max = pack.get("pack_format").getAsInt();
        }
        return min == null || (format >= min && format <= max);
    }

    /** PACP writes {@code "min_format": [20]}; the vanilla form is a bare number. Accept both. */
    private static int scalar(JsonElement element) {
        return element.isJsonArray() ? element.getAsJsonArray().get(0).getAsInt() : element.getAsInt();
    }
}
