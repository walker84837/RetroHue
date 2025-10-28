package org.winlogon.retrohue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Backported version of Bukkit's {@code ChatColor}.
 *
 * This enum represents legacy Minecraft color and formatting codes and provides
 * utilities to work with legacy-coded strings. It is compatible with legacy color
 * codes (the section sign '&#167;' followed by a code character) and integrates with
 * RetroHue for conversions between legacy-coded strings and Adventure's
 * {@link Component}/{@code MiniMessage} representations.
 *
 * @see <a href="https://jd.papermc.io/paper/1.21.10/org/bukkit/ChatColor.html">org.bukkit.ChatColor</a>
 */
public enum ChatColor {
    BLACK('0', "black"),
    DARK_BLUE('1', "dark_blue"),
    DARK_GREEN('2', "dark_green"),
    DARK_AQUA('3', "dark_aqua"),
    DARK_RED('4', "dark_red"),
    DARK_PURPLE('5', "dark_purple"),
    GOLD('6', "gold"),
    GRAY('7', "gray"),
    DARK_GRAY('8', "dark_gray"),
    BLUE('9', "blue"),
    GREEN('a', "green"),
    AQUA('b', "aqua"),
    RED('c', "red"),
    LIGHT_PURPLE('d', "light_purple"),
    YELLOW('e', "yellow"),
    WHITE('f', "white"),
    MAGIC('k', "magic"),
    BOLD('l', "bold"),
    STRIKETHROUGH('m', "strikethrough"),
    UNDERLINE('n', "underline"),
    ITALIC('o', "italic"),
    RESET('r', "reset");

    private static final RetroHue rh;
    private static final Map<Character, ChatColor> COLOR_LOOKUP = new HashMap<>();
    private static final char COLOR_CHAR = '§';
    private static final long VALID_LOW;  // bits 0..63
    private static final long VALID_HIGH; // bits 64..127
    private final char code;
    private final String name;
    private final String toStringCache;

    /**
     * Construct a ChatColor constant.
     *
     * @param code legacy code character (e.g. 'a' for GREEN)
     * @param name canonical name used by RetroHue or MiniMessage conversion
     */
    ChatColor(char code, String name) {
        this.code = code;
        this.name = name;
        this.toStringCache = new String(new char[] { COLOR_CHAR, code });
    }

    /**
     * Returns the legacy code character for this color/formatting constant.
     *
     * @return legacy code character (without the leading section sign)
     */
    public char getCode() {
        return code;
    }

    /**
     * Returns the canonical name associated with this constant.
     *
     * @return name used for MiniMessage/RetroHue mappings (e.g. "red", "bold")
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether this constant represents a color (as opposed to a formatting code).
     *
     * @return {@code true} if this is a color code; {@code false} for formatting/reset
     */
    public boolean isColor() {
        return switch (this) {
            case BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE,
                 GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE,
                 YELLOW, WHITE -> true;
            default -> false;
        };
    }

    /**
     * Returns whether this constant represents a formatting code (bold, italic, etc.).
     *
     * @return {@code true} for formatting codes (k, l, m, n, o); {@code false} otherwise
     */
    public boolean isFormat() {
        return switch (this) {
            case BOLD, STRIKETHROUGH, UNDERLINE, ITALIC, MAGIC -> true;
            default -> false;
        };
    }

    /**
     * Removes legacy color and formatting codes from the given string.
     *
     * <p>Returns {@code null} if {@code input} is {@code null}.</p>
     *
     * @param input string potentially containing legacy color codes
     * @return string with legacy codes removed, or {@code null} if input was {@code null}
     */
    public static @Nullable String stripColor(@Nullable String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("(?i)" + COLOR_CHAR + "[0-9a-fk-or]", "");
    }

    /**
     * Translates alternate color code identifiers (for example '&amp;') to the legacy
     * section sign {@code '&#167;'} codes, but only when followed by a valid code char.
     *
     * <i>This method preserves characters that are not valid legacy code sequences.</i>
     *
     * @param identifier alternate identifier to translate (e.g. '&amp;')
     * @param input non-null input string
     * @return string with translated legacy codes; original string if no translations
     */
    public static @NotNull String translateAlternateColorCodes(char identifier, @NotNull String input) {
        final int length = input.length();
        StringBuilder out = null;
        int lastAppended = 0;

        for (int i = 0; i < length - 1; i++) {
            char curr = input.charAt(i);
            if (curr != identifier) continue;

            char c = input.charAt(i + 1);
            if (!isValidCode(c)) continue;

            if (out == null) out = new StringBuilder(length);
            if (lastAppended < i) out.append(input, lastAppended, i);
            out.append('§').append(c);
            lastAppended = i + 2;
            i++; // skip code char
        }

        if (out == null) return input;
        if (lastAppended < length) out.append(input, lastAppended, length);
        return out.toString();
    }

    // Use a 128-bit bitmask to store all allowed characters
    static {
        long low = 0L, high = 0L;
        String allowed = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";
        for (int i = 0; i < allowed.length(); i++) {
            int v = allowed.charAt(i);
            if (v < 64) low |= 1L << v;
            else high |= 1L << (v - 64);
        }
        VALID_LOW = low;
        VALID_HIGH = high;

        for (ChatColor color : values()) {
            COLOR_LOOKUP.put(color.getCode(), color);
        }

        rh = new RetroHue();
    }

    // Checks whether the current legacy color code (not the identifier) is valid
    private static boolean isValidCode(char c) {
        int v = c;
        if (v < 64) return (VALID_LOW & (1L << v)) != 0;
        if (v < 128) return (VALID_HIGH & (1L << (v - 64))) != 0;
        return false;
    }

    /**
     * Returns the {@code ChatColor} corresponding to the given legacy code character.
     *
     * @param code legacy code character (case-sensitive)
     * @return matching {@code ChatColor}, or {@code null} if no match exists
     */
    public static ChatColor getByChar(char code) {
        for (var color : values()) {
            if (color.code == code) {
                return color;
            }
        }
        return null;
    }

    /**
     * Returns the legacy color/formatting suffixes that are active at the end of the provided input string.
     *
     * The returned string is a concatenation of two-character * legacy codes (e.g. "&#167;a&#167;l")
     * representing the last encountered color/formatting codes in forward order. If none are found,
     * returns an empty string.
     *
     * @param input string to inspect (may be {@code null})
     * @return trailing legacy codes as a string starting with '&#167;', or empty string
     */
    public static String getLastColors(String input) {
        if (input == null || input.isEmpty()) return "";

        int len = input.length();
        // use a small fixed-size array; avoids stringbuilder inserts
        var found = new ChatColor[16];
        int index = 0;

        for (int i = len - 1; i >= 0; i--) {
            if (input.charAt(i) != COLOR_CHAR || i + 1 >= len) continue;

            char code = Character.toLowerCase(input.charAt(i + 1));
            ChatColor color = COLOR_LOOKUP.get(code);
            if (color == null) continue;

            found[index++] = color;

            // stop if we hit a color or reset code
            if (color.isColor() || color == RESET) break;
        }

        if (index == 0) return "";

        // build in forward order
        var sb = new StringBuilder(index * 2);
        for (int i = index - 1; i >= 0; i--) {
            sb.append(found[i].toString());
        }

        return sb.toString();
    }

    /**
     * Converts a legacy-coded string into an Adventure {@link Component}.
     *
     * The conversion uses the internal {@link RetroHue} instance to first translate
     * legacy codes into MiniMessage format and then deserialize that MiniMessage into
     * an Adventure {@code Component}.
     *
     * @param legacy string containing legacy color/formatting codes (uses '&#167;')
     * @return parsed {@link Component} representing the same styling
     */
    public static @NotNull Component asComponent(@NotNull String legacy) {
        var convertedString = rh.convertToMiniMessage(legacy, COLOR_CHAR);
        return rh.getMiniMessage().deserialize(convertedString);
    }

    /**
     * Converts this color to an Adventure {@link NamedTextColor} when applicable.
     *
     * <p>Formatting codes (bold, italic, etc.) and {@link #RESET} have no direct color
     * equivalent and will return {@code null}.</p>
     *
     * @return corresponding {@link NamedTextColor} for color constants, or nothing for formatting/reset constants
     */
    public Optional<NamedTextColor> toNamedTextColor() {
        return switch (this) {
            case BLACK -> Optional.of(NamedTextColor.BLACK);
            case DARK_BLUE -> Optional.of(NamedTextColor.DARK_BLUE);
            case DARK_GREEN -> Optional.of(NamedTextColor.DARK_GREEN);
            case DARK_AQUA -> Optional.of(NamedTextColor.DARK_AQUA);
            case DARK_RED -> Optional.of(NamedTextColor.DARK_RED);
            case DARK_PURPLE -> Optional.of(NamedTextColor.DARK_PURPLE);
            case GOLD -> Optional.of(NamedTextColor.GOLD);
            case GRAY -> Optional.of(NamedTextColor.GRAY);
            case DARK_GRAY -> Optional.of(NamedTextColor.DARK_GRAY);
            case BLUE -> Optional.of(NamedTextColor.BLUE);
            case GREEN -> Optional.of(NamedTextColor.GREEN);
            case AQUA -> Optional.of(NamedTextColor.AQUA);
            case RED -> Optional.of(NamedTextColor.RED);
            case LIGHT_PURPLE -> Optional.of(NamedTextColor.LIGHT_PURPLE);
            case YELLOW -> Optional.of(NamedTextColor.YELLOW);
            case WHITE -> Optional.of(NamedTextColor.WHITE);
            default -> Optional.empty();
        };
    }

    @Override
    public String toString() {
        return toStringCache;
    }
}
