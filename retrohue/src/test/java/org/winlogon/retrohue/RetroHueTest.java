package org.winlogon.retrohue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.format.NamedTextColor;

import static org.junit.jupiter.api.Assertions.*;

class RetroHueTest {
    @Test
    @DisplayName("convertToMiniMessage() should correctly convert ampersand color codes to MiniMessage format")
    void convertMiniMessageAmpersandString() {
        var rh = new RetroHue();
        var contents = "&aHello!";
        var miniMessageString = rh.convertToMiniMessage(contents, '&');
        assertEquals("<green>Hello!</green>", miniMessageString);
    }

    @Test
    @DisplayName("convertColorCode() should return correct NamedTextColor for valid code")
    void testColorCodeConversion() {
        var rh = new RetroHue();
        var opt = rh.convertColorCode("&a", '&');
        assertTrue(opt.isPresent(), "Expected &a to produce a valid NamedTextColor");
        assertEquals(NamedTextColor.GREEN, opt.get());
    }

    @Test
    @DisplayName("convertColorCode() should return empty Optional for strings with incorrect length")
    void testColorCodeOfIncorrectLength() {
        var rh = new RetroHue();
        var opt = rh.convertColorCode("abc123", '&');
        assertTrue(opt.isEmpty(), "Invalid-length code should return an empty Optional");
    }

    @Test
    @DisplayName("convertColorCode() should return empty Optional for invalid color codes")
    void testProvideInvalidColorCode() {
        var rh = new RetroHue();
        var opt = rh.convertColorCode("&p", '&');
        assertTrue(opt.isEmpty(), "Invalid color code (&p) should return an empty Optional");
    }
}
