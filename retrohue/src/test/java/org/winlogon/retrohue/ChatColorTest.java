package org.winlogon.retrohue;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class ChatColorTest {
    @Test
    @DisplayName("toString() should return section character followed by code")
    void testToStringFormat() {
        assertEquals("§a", ChatColor.GREEN.toString());
        assertEquals("§l", ChatColor.BOLD.toString());
    }

    @Test
    @DisplayName("isColor() should correctly identify color codes")
    void testIsColor() {
        assertTrue(ChatColor.RED.isColor());
        assertTrue(ChatColor.BLUE.isColor());
        assertFalse(ChatColor.BOLD.isColor());
        assertFalse(ChatColor.ITALIC.isColor());
    }

    @Test
    @DisplayName("isFormat() should correctly identify formatting codes")
    void testIsFormat() {
        assertTrue(ChatColor.BOLD.isFormat());
        assertTrue(ChatColor.ITALIC.isFormat());
        assertFalse(ChatColor.GREEN.isFormat());
        assertFalse(ChatColor.RESET.isFormat());
    }

    @Test
    @DisplayName("getByChar() should return correct enum or null for invalid code")
    void testGetByChar() {
        assertEquals(ChatColor.RED, ChatColor.getByChar('c'));
        assertEquals(ChatColor.GREEN, ChatColor.getByChar('a'));
        assertNull(ChatColor.getByChar('z'));
    }

    @Test
    @DisplayName("stripColor() should remove color codes from text")
    void testStripColor() {
        String input = "§aHello §bWorld§r!";
        String expected = "Hello World!";
        assertEquals(expected, ChatColor.stripColor(input));
    }

    @Test
    @DisplayName("getLastColors() should return last color codes correctly")
    void testGetLastColors() {
        String input = "§aHello §bWorld §lBold";
        String expected = org.bukkit.ChatColor.getLastColors(input);
        String result = ChatColor.getLastColors(input);
        assertEquals(expected, result);

        input = "§aGreen text §lBold now";
        result = ChatColor.getLastColors(input);
        expected = org.bukkit.ChatColor.getLastColors(input);
        assertEquals(expected, result);

        input = "§aGreen §bBlue";
        result = ChatColor.getLastColors(input);
        expected = org.bukkit.ChatColor.getLastColors(input);
        assertEquals(expected, result);

        input = "Hello plain text";
        result = ChatColor.getLastColors(input);
        expected = org.bukkit.ChatColor.getLastColors(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("getLastColors() should stop at reset or color")
    void testGetLastColorsStopsAtResetOrColor() {
        String input = "§aGreen §lBold §rReset";
        String result = ChatColor.getLastColors(input);
        assertEquals("§r", result);
    }

    @Test
    @DisplayName("asComponent() should convert legacy codes to Component text")
    void testAsComponent() {
        Component component = ChatColor.asComponent("§aHello §lWorld");
        String miniMsg = component.toString();
        assertTrue(miniMsg.contains("Hello"));
        assertTrue(miniMsg.contains("World"));
    }

    @Test
    @DisplayName("translateAlternateColorCodes() should replace & with section")
    void testTranslateAlternateColorCodes() throws Exception {
        // Using reflection since the method is private
        var method = ChatColor.class.getDeclaredMethod("translateAlternateColorCodes", char.class, String.class);
        method.setAccessible(true);

        String input = "&aHello &bWorld";
        String output = (String) method.invoke(null, '&', input);

        assertEquals("§aHello §bWorld", output);
    }

    @Test
    @DisplayName("stripColor() should handle null safely")
    void testStripColorNull() {
        assertNull(ChatColor.stripColor(null));
    }

    @Test
    @DisplayName("getLastColors() should handle empty input safely")
    void testGetLastColorsEmpty() {
        assertEquals("", ChatColor.getLastColors(""));
        assertEquals("", ChatColor.getLastColors(null));
    }
}

