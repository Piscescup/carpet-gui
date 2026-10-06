package io.github.piscescup.fabricmc.carpetgui.gui.model;

import java.util.regex.Pattern;

/** Human-readable English titles without changing the stable rule/command identifier. */
public final class RuleNames {
    private static final Pattern WORD_BOUNDARY = Pattern.compile(
        "(?<=\\p{Lu})(?=\\p{Lu}\\p{Ll})|(?<=[\\p{Ll}\\p{Nd}])(?=\\p{Lu})"
    );
    private static final Pattern SEPARATOR = Pattern.compile("[_\\-\\s]+");

    private RuleNames() {}

    public static String displayName(String ruleId) {
        String words = SEPARATOR.matcher(WORD_BOUNDARY.matcher(ruleId).replaceAll(" ")).replaceAll(" ").trim();
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
