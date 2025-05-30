package org.winlogon.retrohue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
     * @returns The converted string
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
      * Converts a legacy color code to a {@link net.kyori.adventure.text.format.NamedTextColor}, if valid.
      * By default, it uses the & color code prefix.
      *
      * @param code Code to convert. Should be only two characters for character and prefix, like <code>&a</code>
      * @returns The {@link net.kyori.adventure.text.format.NamedTextColor} if valid, or else nothing.
      */
    public Optional<NamedTextColor> convertColorCode(String code) {
        return convertColorCode(code, '&');
    }

    /**
      * Converts a legacy color code (0-9 and A-F) to a {@link net.kyori.adventure.text.format.NamedTextColor}, if valid.
      *
      * @param code Code to convert. Should be only two characters for character and prefix, like <code>&a</code>
      * @param prefix The prefix of the code.
      * @returns The {@link net.kyori.adventure.text.format.NamedTextColor} if valid, or else nothing.
      */
    public Optional<NamedTextColor> convertColorCode(String code, char prefix) {
        if (code.length() != 2 || code.charAt(0) != prefix) {
            return Optional.empty();
        }
    
        switch (code.charAt(1)) {
            case '0': return Optional.of(NamedTextColor.BLACK);
            case '1': return Optional.of(NamedTextColor.DARK_BLUE);
            case '2': return Optional.of(NamedTextColor.DARK_GREEN);
            case '3': return Optional.of(NamedTextColor.DARK_AQUA);
            case '4': return Optional.of(NamedTextColor.DARK_PURPLE);
            case '5': return Optional.of(NamedTextColor.DARK_RED);
            case '6': return Optional.of(NamedTextColor.GOLD);
            case '7': return Optional.of(NamedTextColor.GRAY);
            case '8': return Optional.of(NamedTextColor.DARK_GRAY);
            case '9': return Optional.of(NamedTextColor.BLUE);
            case 'a': return Optional.of(NamedTextColor.GREEN);
            case 'b': return Optional.of(NamedTextColor.AQUA);
            case 'c': return Optional.of(NamedTextColor.RED);
            case 'd': return Optional.of(NamedTextColor.LIGHT_PURPLE);
            case 'e': return Optional.of(NamedTextColor.YELLOW);
            case 'f': return Optional.of(NamedTextColor.WHITE);
            default: return Optional.empty();
        }
    }

    /**
     * Converts a legacy-code formatted string into a {@link net.kyori.adventure.text.Component}
     *
     * @param content The raw content
     */
    public Component convertToComponent(String content) {
        var message = convertToMiniMessage(content);
        return this.mm.deserialize(message);
    }
}
