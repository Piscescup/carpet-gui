/*
 * This file is part of the Carpet GUI project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Fallen_Breath and contributors
 *
 * Carpet GUI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Carpet GUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Carpet GUI.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.piscescup.fabricmc.carpetgui.util;

import java.util.regex.Pattern;

/** Human-readable English titles without changing the stable rule/command identifier. */
public final class RuleNameFmtUtils {
    private static final Pattern WORD_BOUNDARY = Pattern.compile(
        "(?<=\\p{Lu})(?=\\p{Lu}\\p{Ll})|(?<=[\\p{Ll}\\p{Nd}])(?=\\p{Lu})"
    );
    private static final Pattern SEPARATOR = Pattern.compile("[_\\-\\s]+");

    private RuleNameFmtUtils() {}

    public static String displayName(String ruleId) {
        String words = SEPARATOR.matcher(WORD_BOUNDARY.matcher(ruleId)
                .replaceAll(" ")
            )
            .replaceAll(" ")
            .trim();
        if (words.isEmpty()) return words;

        StringBuilder title = new StringBuilder();

        for (String word : words.split(" ")) {
            if (!title.isEmpty()) title.append(' ');
            int first = word.codePointAt(0);
            title.appendCodePoint(Character.toUpperCase(first)).append(word.substring(Character.charCount(first)));
        }
        return title.toString();
    }
}
