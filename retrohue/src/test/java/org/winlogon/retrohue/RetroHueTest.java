package org.winlogon.retrohue;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetroHueTest {
    @Test void convertMiniMessageAmpersandString() {
        var rh = new RetroHue();
        var contents = "&aHello!";
        var miniMessageString = rh.convertToMiniMessage(contents, '&');
        System.out.println("Message: " + miniMessageString);
        assertTrue(miniMessageString.equals("<green>Hello!</green>"), "&a is <green> in MiniMessage.");
    }
}
