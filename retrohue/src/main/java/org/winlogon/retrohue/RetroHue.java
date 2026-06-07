package org.winlogon.retrohue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
    private final boolean enableHexColors;
    private final boolean enableEscapes;
    private final boolean stripObfuscated;
    private final char hexCharacter;

    // Map legacy color codes (0-9, a-f) to MiniMessage color names
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

    private RetroHue(Builder builder) {
        this.mm = builder.mm;
        this.enableHexColors = builder.enableHexColors;
        this.enableEscapes = builder.enableEscapes;
        this.stripObfuscated = builder.stripObfuscated;
        this.hexCharacter = builder.hexCharacter;
    }

    public RetroHue() {
        this(new Builder());
    }

    public RetroHue(MiniMessage mm) {
        this(new Builder().mm(mm));
    }

    /**
     * Creates a new builder for configuring RetroHue.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the internal MiniMessage instance used for serialization and deserialization.
     *
     * @return The internal {@link MiniMessage} instance used by this RetroHue.
     */
    public MiniMessage getMiniMessage() {
        return this.mm;
    }

    /**
     * Convert a string containing Minecraft-style legacy codes (using '&sect;') into a MiniMessage string.
     *
     * @param content The raw text
     * @return The converted string
     */
    public String convertToMiniMessage(String content) {
        return convertToMiniMessage(content, '§');
    }

    /**
     * Convert a string containing Minecraft-style legacy codes into a MiniMessage string.
     *
     * @param content         The raw text, possibly containing sequences like "&sect;a" (or "{codeIdentifier}a") for colors/formats
     * @param codeIdentifier  The character that precedes each legacy code (often '&sect;', but some plugins use '&amp;' or another marker)
     * @return                A MiniMessage-compatible string, e.g. <code>&lt;green&gt;This is green &lt;bold&gt;and bold&lt;/bold&gt;&lt;/green&gt;</code>
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

            // Handle escape sequences: && -> &, §§ -> §
            if (enableEscapes && i + 1 < content.length() && content.charAt(i + 1) == codeIdentifier) {
                result.append(codeIdentifier);
                i++;
                continue;
            }

            char codeChar = Character.toLowerCase(content.charAt(i + 1));
            i++;

            // Color codes take precedence over normal formatting codes
            // At this point `i` points to the color introducer (#, x, etc.), so the actual hexadecimal
            // digits begin at i + 1
            if (enableHexColors && isHexColorStart(codeChar, content, i + 1, codeIdentifier)) {
                String hexColor = parseHexColor(content, i + 1, codeIdentifier);
                if (hexColor != null) {

                    // A new color replaces the currently active color, but
                    // leaves any active formatting tags (bold, italic, etc.) intact
                    if (currentColorTag != null) {
                        result.append("</").append(currentColorTag).append(">");
                        if (!openTags.isEmpty() && openTags.peek().equals(currentColorTag)) {
                            openTags.pop();
                        }
                    }

                    // Open the new color tag and make it the active color state
                    currentColorTag = "#" + hexColor;
                    result.append("<").append(currentColorTag).append(">");
                    openTags.push(currentColorTag);

                    // Skip over the characters that form the hex color so they
                    // are not processed again by the main loop
                    if (codeChar == hexCharacter) {
                        i += 6; // &#RRGGBB - 6 hex digits
                    } else if (codeChar == 'x') {
                        // Support both:
                        // - &xRRGGBB
                        // - &x&R&R&G&G&B&B
                        // The long format contains an additional code identifier
                        // before each hex digit, so it consumes more characters
                        if (i + 1 < content.length() && content.charAt(i + 1) == codeIdentifier) {
                            i += 12;
                        } else {
                            i += 6;
                        }
                    }
                    continue;
                }
            }

            // Standard color code: replace the current color while preserving
            // any non-color formatting already applied
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

            // Formatting codes (bold, italic, underline, etc.) stack on top of
            // existing formatting instead of replacing it
            } else if (FORMAT_MAP.containsKey(codeChar)) {
                if (stripObfuscated && codeChar == 'k') {
                    continue;
                }
                String formatTag = FORMAT_MAP.get(codeChar);

                // Prevent duplicate nested tags such as <bold><bold>
                if (!openTags.contains(formatTag)) {
                    result.append("<").append(formatTag).append(">");
                    openTags.push(formatTag);
                }

            // Reset code: close every currently open tag and return to the default formatting state
            } else if (codeChar == 'r') {
                while (!openTags.isEmpty()) {
                    String tagToClose = openTags.pop();
                    result.append("</").append(tagToClose).append(">");
                }
                currentColorTag = null;
            // Unknown code sequence; preserve it literally in the output
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

    private boolean isHexColorStart(char codeChar, String content, int index, char codeIdentifier) {
        if (codeChar != hexCharacter && codeChar != 'x') {
            return false;
        }

        // Kyori format: &#RRGGBB - index points to first hex digit, need 6 chars
        if (codeChar == hexCharacter) {
            return content.length() - index >= 6 && isHexDigits(content.substring(index, index + 6));
        }

        // BungeeCord format: &xRRGGBB (short: 6 hex digits) or &x&R&R&G&G&B&B (long: 6 pairs)
        // index points to first char after 'x'
        if (content.length() - index < 6) {
            return false;
        }
        // Check if it's long format (next char is codeIdentifier)
        if (content.length() - index >= 12 && content.charAt(index) == codeIdentifier) {
            return true; // long format
        }
        // Short format: 6 hex digits directly after x
        return isHexDigits(content.substring(index, index + 6));
    }

    private boolean isHexDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private String parseHexColor(String content, int index, char codeIdentifier) {
        char codeChar = Character.toLowerCase(content.charAt(index - 1));

        if (codeChar == hexCharacter) {
            // Kyori format: &#RRGGBB - index points to first hex digit
            if (content.length() - index >= 6) {
                String hex = content.substring(index, index + 6);
                if (isHexDigits(hex)) {
                    return hex.toLowerCase();
                }
            }
        } else if (codeChar == 'x') {
            // BungeeCord format: index points to first char after 'x'
            // Short format: &xRRGGBB (6 hex digits directly)
            // Long format: &x&R&R&G&G&B&B (6 pairs of prefix+hexdigit)
            if (content.length() - index < 6) {
                return null;
            }
            // Check for long format (next char is codeIdentifier)
            if (content.charAt(index) == codeIdentifier) {
                // Long format: &x&f&f&0&0&0&0
                if (content.length() - index < 12) {
                    return null;
                }
                StringBuilder hex = new StringBuilder(6);
                int pos = index;
                for (int pair = 0; pair < 6 && pos + 1 < content.length(); pair++) {
                    char prefix = content.charAt(pos);
                    char hexDigit = Character.toLowerCase(content.charAt(pos + 1));
                    if (prefix != codeIdentifier || !isHexDigit(hexDigit)) {
                        return null;
                    }
                    hex.append(hexDigit);
                    pos += 2;
                }
                if (hex.length() == 6) {
                    return hex.toString();
                }
            } else {
                // Short format: &xff0000
                String hex = content.substring(index, index + 6);
                if (isHexDigits(hex)) {
                    return hex.toLowerCase();
                }
            }
        }
        return null;
    }

    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    /**
     * Converts a legacy color code to a {@link NamedTextColor}, if valid.
     * By default, it uses the ampersand color code prefix.
     *
     * @param code Code to convert. Should be only two characters for character and prefix, like <code>&amp;a</code>
     * @return The {@link NamedTextColor} if valid, or else nothing.
     */
    public Optional<NamedTextColor> convertColorCode(String code) {
        return convertColorCode(code, '&');
    }

    /**
     * Converts a legacy color code (0-9 and A-F) to a {@link NamedTextColor}, if valid.
     *
     * @param code Code to convert. Should be only two characters for character and prefix, like <code>&amp;a</code>
     * @param prefix The prefix of the code.
     * @return The {@link NamedTextColor} if valid, or else nothing.
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
     * Converts a legacy hex color code to a {@link TextColor}, if valid.
     * Supports both Kyori format ({@code &#RRGGBB}) and BungeeCord format ({@code &xRRGGBB}).
     *
     * @param code Code to convert, e.g. {@code &#ff0000} or {@code &xff0000}
     * @return The {@link TextColor} if valid, or else nothing.
     */
    public Optional<TextColor> convertHexCode(String code) {
        return convertHexCode(code, '&');
    }

    /**
     * Converts a legacy hex color code to a {@link TextColor}, if valid.
     * Supports both Kyori format ({@code &#RRGGBB}) and BungeeCord format ({@code &xRRGGBB}).
     *
     * @param code Code to convert, e.g. {@code &#ff0000} or {@code &xff0000}
     * @param prefix The prefix character (e.g. {@code &} or {@code §})
     * @return The {@link TextColor} if valid, or else nothing.
     */
    public Optional<TextColor> convertHexCode(String code, char prefix) {
        if (code == null || code.length() < 3 || code.charAt(0) != prefix) {
            return Optional.empty();
        }

        char codeChar = Character.toLowerCase(code.charAt(1));

        // Kyori format: &#RRGGBB
        if (codeChar == hexCharacter && code.length() == 8) {
            String hex = code.substring(2, 8);
            if (isHexDigits(hex)) {
                try {
                    return Optional.of(TextColor.fromHexString("#" + hex));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // BungeeCord format: &xRRGGBB (8 chars) or &x&R&R&G&G&B&B (14 chars)
        if (codeChar == 'x') {
            // Short form: &xRRGGBB
            if (code.length() == 8) {
                String hex = code.substring(2, 8);
                if (isHexDigits(hex)) {
                    try {
                        return Optional.of(TextColor.fromHexString("#" + hex));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            // Long form: &x&R&R&G&G&B&B (each hex digit prefixed)
            if (code.length() == 14) {
                StringBuilder hex = new StringBuilder(6);
                for (int i = 2; i < 14; i += 2) {
                    if (code.charAt(i) != prefix) {
                        return Optional.empty();
                    }
                    char hexDigit = Character.toLowerCase(code.charAt(i + 1));
                    if (!isHexDigit(hexDigit)) {
                        return Optional.empty();
                    }
                    hex.append(hexDigit);
                }
                try {
                    return Optional.of(TextColor.fromHexString("#" + hex));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Converts a hex color code to a {@link NamedTextColor}, if valid.
     *
     * @param hex Hex color code, like <code>#ff0000</code>
     * @return The {@link NamedTextColor} if valid, or else nothing.
     */
    public Optional<NamedTextColor> getNamedColorFromHex(String hex) {
        if (!seemsHexadecimal(hex) || isNullOrEmpty(hex)) {
            return Optional.empty();
        }

        var textColorOpt = Optional.ofNullable(TextColor.fromHexString(hex));

        if (textColorOpt.isEmpty()) {
            return Optional.empty();
        }

        var nearestColor = NamedTextColor.nearestTo(textColorOpt.get());
        return Optional.of(nearestColor);
    }

    private boolean seemsHexadecimal(String s) {
        return s.startsWith("#") && s.length() == 7;
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

     /**
      * Converts a legacy-code formatted string into a MiniMessage string, and then into a {@link Component}, with a section character identifier
      *
      * @param content The raw content
      * @return The converted component
      */
    public Component convertToComponent(String content) {
        var message = convertToMiniMessage(content);
        return this.mm.deserialize(message);
    }

     /**
      * Converts a legacy-code formatted string into a MiniMessage string, and then into a {@link Component}, given a starting code character identifier
      *
      * @param content The raw content
      * @param codeIdentifier The character that precedes each legacy code (often '&sect;', but some plugins use '&amp;' or another marker)
      * @return The converted component
      */
    public Component convertToComponent(String content, char codeIdentifier) {
        var message = convertToMiniMessage(content, codeIdentifier);
        return this.mm.deserialize(message);
    }

    /**
     * Builder for configuring RetroHue instances.
     */
    public static class Builder {
        private MiniMessage mm = MiniMessage.miniMessage();
        private boolean enableHexColors = true;
        private boolean enableEscapes = true;
        private boolean stripObfuscated = false;
        private char hexCharacter = '#';

        private Builder() {}

        /**
         * Sets the MiniMessage instance to use for deserialization.
         *
         * @param mm the MiniMessage instance
         * @return this builder
         */
        public Builder mm(MiniMessage mm) {
            this.mm = mm;
            return this;
        }

        /**
         * Enables or disables parsing of hex color codes ({@code &#RRGGBB} and {@code &xRRGGBB}).
         * Default: {@code true}
         *
         * @param enableHexColors whether to enable hex color parsing
         * @return this builder
         */
        public Builder hexColors(boolean enableHexColors) {
            this.enableHexColors = enableHexColors;
            return this;
        }

        /**
         * Enables or disables escape sequence handling ({@code &&} → {@code &}, {@code §§} → {@code §}).
         * Default: {@code true}
         *
         * @param enableEscapes whether to enable escape sequences
         * @return this builder
         */
        public Builder escapes(boolean enableEscapes) {
            this.enableEscapes = enableEscapes;
            return this;
        }

        /**
         * Enables or disables stripping of obfuscated/magic formatting codes ({@code &k}/{@code §k}).
         * Default: {@code false}
         *
         * @param stripObfuscated whether to strip obfuscated codes
         * @return this builder
         */
        public Builder stripObfuscated(boolean stripObfuscated) {
            this.stripObfuscated = stripObfuscated;
            return this;
        }

        /**
         * Sets the hex color prefix character for Kyori-format hex codes.
         * Default: {@code '#'}
         *
         * @param hexCharacter the hex prefix character
         * @return this builder
         */
        public Builder hexCharacter(char hexCharacter) {
            this.hexCharacter = hexCharacter;
            return this;
        }

        /**
         * Builds the RetroHue instance.
         *
         * @return a new RetroHue instance
         */
        public RetroHue build() {
            return new RetroHue(this);
        }
    }
}
