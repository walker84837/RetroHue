package org.winlogon.retrohue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * RetroHue - Convert Minecraft legacy codes to MiniMessage
 */
public class RetroHue {
    private final MiniMessage mm;

    // Map legacy color codes (0–9, a–f) to MiniMessage color names
    private static final Map<Character, String> COLOR_MAP = new HashMap<>() {{
        put('0', "black");
        put('1', "dark_blue");
        put('2', "dark_green");
        put('3', "dark_aqua");
        put('4', "dark_red");
        put('5', "dark_purple");
        put('6', "gold");
        put('7', "gray");
        put('8', "dark_gray");
        put('9', "blue");
        put('a', "green");
        put('b', "aqua");
        put('c', "red");
        put('d', "light_purple");
        put('e', "yellow");
        put('f', "white");
    }};

    // Map legacy format codes (k, l, m, n, o) to MiniMessage tags
    private static final Map<Character, String> FORMAT_MAP = new HashMap<>() {{
        put('k', "obfuscated");
        put('l', "bold");
        put('m', "strikethrough");
        put('n', "underlined");
        put('o', "italic");
    }};

    public RetroHue() {
        this.mm = MiniMessage.miniMessage();
    }

    public RetroHue(MiniMessage mm) {
        this.mm = mm;
    }

    /**
     * Convert a string containing Minecraft-style legacy codes (using '§') into a MiniMessage string.
     *
     * @param content The raw text
     */
    public String convertToMiniMessage(String content) {
        return convertToMiniMessage(content, '§');
    }

    /**
     * Convert a string containing Minecraft-style legacy codes into a MiniMessage string.
     *
     * @param content         The raw text, possibly containing sequences like "§a" (or "{codeIdentifier}a") for colors/formats
     * @param codeIdentifier  The character that precedes each legacy code (often '§', but some plugins use '&' or another marker)
     * @return                A MiniMessage-compatible string, e.g. "<green>This is green <bold>and bold</bold></green>"
     */
    public String convertToMiniMessage(String content, char codeIdentifier) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        var result = new StringBuilder();
        // We keep a stack of open tags so that we can close them in reverse order
        // Whenever a color code appears, we pop any active color first
        // Whenever a format code appears, we push its tag onto the stack (unless already present)
        // On reset ('r'), we close everything in reverse order
        Deque<String> openTags = new ArrayDeque<>();

        // Tracks the current active color so we know which tag to close when a new color appears
        String currentColorTag = null;

        for (int i = 0; i < content.length(); i++) {
            var c = content.charAt(i);

            // If this char is the identifier, and there is at least
            // one more character -> attempt code lookup
            boolean isIdentifier = c == codeIdentifier && i + 1 < content.length();
            if (!isIdentifier) {
                result.append(c);
                continue;
            }
            char codeChar = Character.toLowerCase(content.charAt(i + 1));
            i++;

            if (COLOR_MAP.containsKey(codeChar)) {
                if (currentColorTag != null) {
                    result.append("</").append(currentColorTag).append(">");
                    if (!openTags.isEmpty() && openTags.peek().equals(currentColorTag)) {
                        openTags.pop();
                    }
                }
                currentColorTag = COLOR_MAP.get(codeChar);
                result.append("<").append(currentColorTag).append(">");
                openTags.push(currentColorTag);
            } else if (FORMAT_MAP.containsKey(codeChar)) {
                String formatTag = FORMAT_MAP.get(codeChar);
                if (!openTags.contains(formatTag)) {
                    result.append("<").append(formatTag).append(">");
                    openTags.push(formatTag);
                }
            } else if (codeChar == 'r') {
                while (!openTags.isEmpty()) {
                    String tagToClose = openTags.pop();
                    result.append("</").append(tagToClose).append(">");
                }
                currentColorTag = null;
            } else {
                result.append(c).append(codeChar);
            }
        }

        // At the end, close any tags still open
        while (!openTags.isEmpty()) {
            String tagToClose = openTags.pop();
            result.append("</").append(tagToClose).append(">");
        }

        return result.toString();
    }

    /**
     * Wraps convertToMiniMessage(...) and returns a Component via MiniMessage.deserialize(...)
     */
    public Component convertToComponent(String content) {
        var message = convertToMiniMessage(content);
        return this.mm.deserialize(message);
    }
}
