package org.winlogon.retrohue;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.format.NamedTextColor;

import static org.junit.jupiter.api.Assertions.*;

class RetroHueTest {
    @Test void convertMiniMessageAmpersandString() {
        var rh = new RetroHue();
        var contents = "&aHello!";
        var miniMessageString = rh.convertToMiniMessage(contents, '&');
        assertEquals("<green>Hello!</green>", miniMessageString);
    }

    @Test void testColorCodeConversion() {
        var rh = new RetroHue();
        var opt = rh.convertColorCode("&a", '&');
        assert(opt.isPresent());
        assertEquals(opt.get(), NamedTextColor.GREEN);
    }

    @Test void testColorCodeOfIncorrectLength() {
        var rh = new RetroHue();
        var opt = rh.convertColorCode("abc123", '&');
        assert(opt.isEmpty());
    }

    @Test void testProvideInvalidColorCode() {
        var rh = new RetroHue();
        var opt = rh.convertColorCode("&p", '&');
        assert(opt.isEmpty());
    }
}
