package org.winlogon.retrohue;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetroHueTest {
    @Test void convertMiniMessageAmpersandString() {
        var rh = new RetroHue();
        var contents = "&aHello!";
        var miniMessageString = rh.convertToMiniMessage(contents, '&');
        assertTrue(miniMessageString.equals("<green>Hello!"), "&a is <green> in MiniMessage.");
    }
}
