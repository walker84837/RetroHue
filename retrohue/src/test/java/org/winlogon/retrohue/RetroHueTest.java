package org.winlogon.retrohue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import static org.junit.jupiter.api.Assertions.*;

class RetroHueTest {
    @Test
    @DisplayName("DEBUG: test hex parsing internals")
    void testDebugHexParsing() {
        var rh = new RetroHue();
        String content = "&#ff0000Hello!";
        System.out.println("Input: " + content);
        var result = rh.convertToMiniMessage(content, '&');
        System.out.println("Output: " + result);
    }

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

    // --- Hex Color Tests ---

    @ParameterizedTest
    @ValueSource(strings = {"ff0000", "FF0000", "00ff00", "0000ff", "abcdef", "123456"})
    @DisplayName("convertToMiniMessage() should convert Kyori-format hex colors (& #RRGGBB)")
    void testKyoriHexColors(String hexCode) {
        var rh = new RetroHue();
        var input = "&#" + hexCode + "Hello!";
        var result = rh.convertToMiniMessage(input, '&');
        System.out.println("Kyori input: " + input + " => output: " + result);
        assertTrue(result.startsWith("<#"), "Expected MiniMessage hex tag, got: " + result);
        assertTrue(result.contains("Hello!"), "Expected Hello! in output, got: " + result);
        assertTrue(result.endsWith("</#" + hexCode.toLowerCase() + ">"), "Expected closing tag, got: " + result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"&xff0000", "&xFF0000", "&x00ff00", "&x0000ff", "&xabcdef", "&x123456"})
    @DisplayName("convertToMiniMessage() should convert BungeeCord short-format hex colors (&xRRGGBB)")
    void testBungeeShortHexColors(String hexCode) {
        var rh = new RetroHue();
        var input = hexCode + "Hello!";
        var result = rh.convertToMiniMessage(input, '&');
        String expectedHex = hexCode.substring(2).toLowerCase();
        assertTrue(result.startsWith("<#"), "Expected MiniMessage hex tag, got: " + result);
        assertTrue(result.contains("Hello!"), "Expected Hello! in output, got: " + result);
        assertTrue(result.endsWith("</#" + expectedHex + ">"), "Expected closing tag, got: " + result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"&x&f&f&0&0&0&0", "&x&F&F&0&0&0&0", "&x&0&0&f&f&0&0", "&x&0&0&0&0&f&f", "&x&a&b&c&d&e&f"})
    @DisplayName("convertToMiniMessage() should convert BungeeCord long-format hex colors (&x&R&R&G&G&B&B)")
    void testBungeeLongHexColors(String hexCode) {
        var rh = new RetroHue();
        var input = hexCode + "Hello!";
        var result = rh.convertToMiniMessage(input, '&');
        // Extract hex from long format: &x&f&f&0&0&0&0 -> ff0000
        StringBuilder expectedHex = new StringBuilder();
        for (int i = 3; i < hexCode.length(); i += 2) {
            expectedHex.append(Character.toLowerCase(hexCode.charAt(i)));
        }
        assertTrue(result.startsWith("<#"), "Expected MiniMessage hex tag, got: " + result);
        assertTrue(result.contains("Hello!"), "Expected Hello! in output, got: " + result);
        assertTrue(result.endsWith("</#" + expectedHex + ">"), "Expected closing tag, got: " + result);
    }

    @Test
    @DisplayName("convertToMiniMessage() should handle section sign prefix for hex colors")
    void testSectionSignHexColors() {
        var rh = new RetroHue();
        var input = "§#ff0000Red §x§f§f§0§0§0§0AlsoRed";
        var result = rh.convertToMiniMessage(input);
        assertEquals("<#ff0000>Red </#ff0000><#ff0000>AlsoRed</#ff0000>", result);
    }

    @Test
    @DisplayName("convertToMiniMessage() should close previous color before opening hex color")
    void testHexColorClosesPreviousColor() {
        var rh = new RetroHue();
        var input = "&aGreen &#ff0000Red";
        var result = rh.convertToMiniMessage(input, '&');
        assertEquals("<green>Green </green><#ff0000>Red</#ff0000>", result);
    }

    @Test
    @DisplayName("convertToMiniMessage() should handle hex colors mixed with format codes")
    void testHexWithFormats() {
        var rh = new RetroHue();
        var input = "&#ff0000&lBold Red&r Normal";
        var result = rh.convertToMiniMessage(input, '&');
        assertEquals("<#ff0000><bold>Bold Red</bold></#ff0000> Normal", result);
    }

    @Test
    @DisplayName("convertHexCode() should parse Kyori-format hex codes")
    void testConvertKyoriHexCode() {
        var rh = new RetroHue();
        var opt = rh.convertHexCode("&#ff0000", '&');
        assertTrue(opt.isPresent());
        assertEquals(TextColor.fromHexString("#ff0000"), opt.get());
    }

    @Test
    @DisplayName("convertHexCode() should parse BungeeCord short-format hex codes")
    void testConvertBungeeShortHexCode() {
        var rh = new RetroHue();
        var opt = rh.convertHexCode("&xff0000", '&');
        assertTrue(opt.isPresent());
        assertEquals(TextColor.fromHexString("#ff0000"), opt.get());
    }

    @Test
    @DisplayName("convertHexCode() should parse BungeeCord long-format hex codes")
    void testConvertBungeeLongHexCode() {
        var rh = new RetroHue();
        var opt = rh.convertHexCode("&x&f&f&0&0&0&0", '&');
        assertTrue(opt.isPresent());
        assertEquals(TextColor.fromHexString("#ff0000"), opt.get());
    }

    @Test
    @DisplayName("convertHexCode() should return empty for invalid hex codes")
    void testConvertInvalidHexCode() {
        var rh = new RetroHue();
        assertTrue(rh.convertHexCode("&pff0000", '&').isEmpty());
        assertTrue(rh.convertHexCode("&#gggggg", '&').isEmpty());
        assertTrue(rh.convertHexCode("&xff000", '&').isEmpty()); // too short
        assertTrue(rh.convertHexCode("&xff00000", '&').isEmpty()); // too long
        assertTrue(rh.convertHexCode("xff0000", '&').isEmpty()); // missing prefix
    }

    @Test
    @DisplayName("convertHexCode() should work with section sign prefix")
    void testConvertHexCodeSectionSign() {
        var rh = new RetroHue();
        var opt = rh.convertHexCode("§#ff0000", '§');
        assertTrue(opt.isPresent());
        assertEquals(TextColor.fromHexString("#ff0000"), opt.get());
    }

    // --- Builder Tests ---

    @Test
    @DisplayName("Builder should create instance with default settings")
    void testBuilderDefaults() {
        var rh = RetroHue.builder().build();
        assertNotNull(rh.getMiniMessage());
    }

    @Test
    @DisplayName("Builder hexColors(false) should disable hex color parsing")
    void testBuilderDisableHexColors() {
        var rh = RetroHue.builder().hexColors(false).build();
        var result = rh.convertToMiniMessage("&#ff0000Red", '&');
        assertEquals("&#ff0000Red", result); // Should not parse, treated as literal
    }

    @Test
    @DisplayName("Builder escapes(false) should disable escape sequences")
    void testBuilderDisableEscapes() {
        var rh = RetroHue.builder().escapes(false).build();
        var result = rh.convertToMiniMessage("&&aHello", '&');
        System.out.println("escapes(false) result: " + result);
        // With escapes disabled, && is not an escape sequence.
        // First & has code & (invalid), outputs literal "&&".
        // Second & is consumed as codeChar for first &, so 'a' is literal.
        assertEquals("&&aHello", result);
    }

    @Test
    @DisplayName("Builder escapes(true) should handle && -> &")
    void testBuilderEnableEscapes() {
        var rh = RetroHue.builder().escapes(true).build();
        var result = rh.convertToMiniMessage("&&aHello", '&');
        assertEquals("&aHello", result); // && -> &, then 'a' is literal
    }

    @Test
    @DisplayName("Builder stripObfuscated(true) should remove &k formatting codes")
    void testBuilderStripObfuscated() {
        var rh = RetroHue.builder().stripObfuscated(true).build();
        var result = rh.convertToMiniMessage("&kObfuscated &aGreen", '&');
        System.out.println("stripObfuscated result: " + result);
        // &k formatting is stripped (no <obfuscated> tag), but text "Obfuscated " remains
        assertEquals("Obfuscated <green>Green</green>", result);
    }

    @Test
    @DisplayName("Builder stripObfuscated(false) should keep &k codes")
    void testBuilderKeepObfuscated() {
        var rh = RetroHue.builder().stripObfuscated(false).build();
        var result = rh.convertToMiniMessage("&kObfuscated", '&');
        assertEquals("<obfuscated>Obfuscated</obfuscated>", result);
    }

    @Test
    @DisplayName("Builder custom MiniMessage instance should be used")
    void testBuilderCustomMiniMessage() {
        var customMm = MiniMessage.builder().build();
        var rh = RetroHue.builder().mm(customMm).build();
        assertEquals(customMm, rh.getMiniMessage());
    }

    @Test
    @DisplayName("convertToComponent() should work with hex colors")
    void testConvertToComponentWithHex() {
        var rh = new RetroHue();
        var component = rh.convertToComponent("&#ff0000Red &aWorld", '&');
        assertNotNull(component);
        // Verify it contains both colors by serializing back
        var miniMessage = rh.getMiniMessage().serialize(component);
        System.out.println("Serialized component: " + miniMessage);
        assertTrue(miniMessage.toLowerCase().contains("ff0000"));
        assertTrue(miniMessage.toLowerCase().contains("world"));
    }

    @Test
    @DisplayName("Default constructor should have hex colors and escapes enabled")
    void testDefaultConstructorSettings() {
        var rh = new RetroHue();
        // Hex colors enabled by default
        var hexResult = rh.convertToMiniMessage("&#ff0000Red", '&');
        assertTrue(hexResult.startsWith("<#ff0000>"));

        // Escapes enabled by default
        var escapeResult = rh.convertToMiniMessage("&&aHello", '&');
        assertEquals("&aHello", escapeResult);
    }
}