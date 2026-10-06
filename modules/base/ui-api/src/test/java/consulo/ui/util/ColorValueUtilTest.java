/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.util;

import consulo.ui.color.HSLColor;
import consulo.ui.color.RGBColor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author UNV
 * @since 2026-10-05
 */
public class ColorValueUtilTest {
    @Test
    void fromHex() {
        assertThat(ColorValueUtil.fromHex("#abc")).isEqualTo(new RGBColor(0xAA, 0xBB, 0xCC, 255));
        assertThat(ColorValueUtil.fromHex("abc")).isEqualTo(new RGBColor(0xAA, 0xBB, 0xCC, 255));
        assertThat(ColorValueUtil.fromHex("#ABC")).isEqualTo(new RGBColor(0xAA, 0xBB, 0xCC, 255));
        assertThat(ColorValueUtil.fromHex("#FFF")).isEqualTo(new RGBColor(255, 255, 255, 255));
        assertThat(ColorValueUtil.fromHex("#000")).isEqualTo(new RGBColor(0, 0, 0, 255));

        assertThat(ColorValueUtil.fromHex("#abc8")).isEqualTo(new RGBColor(0xAA, 0xBB, 0xCC, 0x88));
        assertThat(ColorValueUtil.fromHex("abc8")).isEqualTo(new RGBColor(0xAA, 0xBB, 0xCC, 0x88));
        assertThat(ColorValueUtil.fromHex("#FFF8")).isEqualTo(new RGBColor(255, 255, 255, 0x88));
        assertThat(ColorValueUtil.fromHex("#0000")).isEqualTo(new RGBColor(0, 0, 0, 0));

        assertThat(ColorValueUtil.fromHex("#abc123")).isEqualTo(new RGBColor(0xAB, 0xC1, 0x23, 255));
        assertThat(ColorValueUtil.fromHex("abc123")).isEqualTo(new RGBColor(0xAB, 0xC1, 0x23, 255));
        assertThat(ColorValueUtil.fromHex("#ABC123")).isEqualTo(new RGBColor(0xAB, 0xC1, 0x23, 255));
        assertThat(ColorValueUtil.fromHex("#FFFFFF")).isEqualTo(new RGBColor(255, 255, 255, 255));
        assertThat(ColorValueUtil.fromHex("#000000")).isEqualTo(new RGBColor(0, 0, 0, 255));

        assertThat(ColorValueUtil.fromHex("#abc12380")).isEqualTo(new RGBColor(0xAB, 0xC1, 0x23, 0x80));
        assertThat(ColorValueUtil.fromHex("abc12380")).isEqualTo(new RGBColor(0xAB, 0xC1, 0x23, 0x80));
        assertThat(ColorValueUtil.fromHex("#ABC12380")).isEqualTo(new RGBColor(0xAB, 0xC1, 0x23, 0x80));
        assertThat(ColorValueUtil.fromHex("#FFFFFFFF")).isEqualTo(new RGBColor(255, 255, 255, 255));
        assertThat(ColorValueUtil.fromHex("#00000000")).isEqualTo(new RGBColor(0, 0, 0, 0));
    }

    @Test
    void fromHexOrNull() {
        assertThat(ColorValueUtil.fromHexOrNull(null)).isNull();
        assertThat(ColorValueUtil.fromHexOrNull("")).isNull();
        assertThat(ColorValueUtil.fromHexOrNull("#")).isNull();
        assertThat(ColorValueUtil.fromHexOrNull("ab")).isNull();
        assertThat(ColorValueUtil.fromHexOrNull("abc12")).isNull();
        assertThat(ColorValueUtil.fromHexOrNull("abc1234")).isNull();
        assertThat(ColorValueUtil.fromHexOrNull("abc123456")).isNull();

        assertThatThrownBy(() -> ColorValueUtil.fromHex(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Should be String of 3, 4, 6 or 8 chars length.");
        assertThatThrownBy(() -> ColorValueUtil.fromHex("ab"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Should be String of 3, 4, 6 or 8 chars length.");
        assertThatThrownBy(() -> ColorValueUtil.fromHex("abc12"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Should be String of 3, 4, 6 or 8 chars length.");

        assertThatThrownBy(() -> ColorValueUtil.fromHex("#xyz"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("unsupported char at 0:xyz");
        assertThatThrownBy(() -> ColorValueUtil.fromHex("#abz"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("unsupported char at 2:abz");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void toCssColor() {
        assertThat(ColorValueUtil.toCssColor(null)).isNull();

        assertThat(ColorValueUtil.toCssColor(new RGBColor(0xAB, 0xC1, 0x23, 255))).isEqualTo("#ABC123");
        assertThat(ColorValueUtil.toCssColor(new RGBColor(255, 255, 255, 255))).isEqualTo("#FFF");
        assertThat(ColorValueUtil.toCssColor(new RGBColor(0, 0, 0, 255))).isEqualTo("#000");

        assertThat(ColorValueUtil.toCssColor(new RGBColor(0xAB, 0xC1, 0x23, 0))).isEqualTo("rgba(171, 193, 35, 0%)");
        assertThat(ColorValueUtil.toCssColor(new RGBColor(0xAB, 0xC1, 0x23, 0x80))).isEqualTo("rgba(171, 193, 35, 50.2%)");

        assertThat(ColorValueUtil.toCssColor(new HSLColor(12f, 0.34f, 0.56f, 0))).isEqualTo("hsla(12deg, 34%, 56%, 0%)");
        assertThat(ColorValueUtil.toCssColor(new HSLColor(12f, 0.34f, 0.56f, 0x80))).isEqualTo("hsla(12deg, 34%, 56%, 50.2%)");
        assertThat(ColorValueUtil.toCssColor(new HSLColor(123.4f, 0.4561f, 0.7888f, 0xFF))).isEqualTo("hsl(123deg, 45.6%, 78.9%)");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void toHex() {
        assertThat(ColorValueUtil.toHex(new RGBColor(0xAB, 0xC1, 0x23, 255))).isEqualTo("ABC123");
        assertThat(ColorValueUtil.toHex(new RGBColor(255, 255, 255, 255))).isEqualTo("FFFFFF");
        assertThat(ColorValueUtil.toHex(new RGBColor(0, 0, 0, 255))).isEqualTo("000000");
        assertThat(ColorValueUtil.toHex(new RGBColor(0x0A, 0x0B, 0x0C, 255))).isEqualTo("0A0B0C");
    }

    @Test
    void isDark() {
        assertThat(ColorValueUtil.isDark(new RGBColor(0, 0, 0, 255))).isTrue();
        assertThat(ColorValueUtil.isDark(new RGBColor(127, 127, 127, 255))).isTrue();
        assertThat(ColorValueUtil.isDark(new RGBColor(128, 128, 128, 255))).isFalse();
        assertThat(ColorValueUtil.isDark(new RGBColor(255, 255, 255, 255))).isFalse();

        assertThat(ColorValueUtil.isDark(new RGBColor(255, 73, 73, 255))).isTrue();
        assertThat(ColorValueUtil.isDark(new RGBColor(255, 74, 74, 255))).isFalse();

        assertThat(ColorValueUtil.isDark(new RGBColor(0, 217, 0, 255))).isTrue();
        assertThat(ColorValueUtil.isDark(new RGBColor(0, 218, 0, 255))).isFalse();

        assertThat(ColorValueUtil.isDark(new RGBColor(111, 111, 255, 255))).isTrue();
        assertThat(ColorValueUtil.isDark(new RGBColor(112, 112, 255, 255))).isFalse();
    }

    @Test
    void brighter() {
        RGBColor black = new RGBColor(0, 0, 0, 255);
        RGBColor brighterBlack = ColorValueUtil.brighter(black);
        assertThat(brighterBlack.getRed()).isGreaterThan(0);
        assertThat(brighterBlack.getGreen()).isGreaterThan(0);
        assertThat(brighterBlack.getBlue()).isGreaterThan(0);
        assertThat(brighterBlack.getAlpha()).isEqualTo(255);

        RGBColor white = new RGBColor(255, 255, 255, 255);
        assertThat(ColorValueUtil.brighter(white)).isEqualTo(white);

        RGBColor color = new RGBColor(100, 150, 200, 128);
        RGBColor brighter = ColorValueUtil.brighter(color);
        assertThat(brighter.getRed()).isGreaterThanOrEqualTo(color.getRed());
        assertThat(brighter.getGreen()).isGreaterThanOrEqualTo(color.getGreen());
        assertThat(brighter.getBlue()).isGreaterThanOrEqualTo(color.getBlue());
        assertThat(brighter.getAlpha()).isEqualTo(128);
    }

    @Test
    void darker() {
        RGBColor white = new RGBColor(255, 255, 255, 255);
        RGBColor darkerWhite = ColorValueUtil.darker(white);
        assertThat(darkerWhite.getRed()).isLessThan(255);
        assertThat(darkerWhite.getGreen()).isLessThan(255);
        assertThat(darkerWhite.getBlue()).isLessThan(255);
        assertThat(darkerWhite.getAlpha()).isEqualTo(255);

        RGBColor black = new RGBColor(0, 0, 0, 255);
        assertThat(ColorValueUtil.darker(black)).isEqualTo(black);

        RGBColor color = new RGBColor(100, 150, 200, 128);
        RGBColor darker = ColorValueUtil.darker(color);
        assertThat(darker.getRed()).isLessThanOrEqualTo(color.getRed());
        assertThat(darker.getGreen()).isLessThanOrEqualTo(color.getGreen());
        assertThat(darker.getBlue()).isLessThanOrEqualTo(color.getBlue());
        assertThat(darker.getAlpha()).isEqualTo(128);
    }

    @Test
    void shift() {
        RGBColor color = new RGBColor(100, 150, 200, 128);
        RGBColor shifted = (RGBColor) ColorValueUtil.shift(color, 0.5);
        assertThat(shifted.getRed()).isEqualTo(50);
        assertThat(shifted.getGreen()).isEqualTo(75);
        assertThat(shifted.getBlue()).isEqualTo(100);
        assertThat(shifted.getAlpha()).isEqualTo(128);

        RGBColor shiftedUp = (RGBColor) ColorValueUtil.shift(color, 2.0);
        assertThat(shiftedUp.getRed()).isEqualTo(200);
        assertThat(shiftedUp.getGreen()).isEqualTo(255);
        assertThat(shiftedUp.getBlue()).isEqualTo(255);
        assertThat(shiftedUp.getAlpha()).isEqualTo(128);

        RGBColor shiftedDown = (RGBColor) ColorValueUtil.shift(color, -1.0);
        assertThat(shiftedDown.getRed()).isEqualTo(0);
        assertThat(shiftedDown.getGreen()).isEqualTo(0);
        assertThat(shiftedDown.getBlue()).isEqualTo(0);
        assertThat(shiftedDown.getAlpha()).isEqualTo(128);
    }

    @Test
    void mix() {
        RGBColor c1 = new RGBColor(0, 0, 0, 0);
        RGBColor c2 = new RGBColor(255, 255, 255, 255);

        RGBColor mixed0 = (RGBColor) ColorValueUtil.mix(c1, c2, 0);
        assertThat(mixed0.getRed()).isEqualTo(0);
        assertThat(mixed0.getGreen()).isEqualTo(0);
        assertThat(mixed0.getBlue()).isEqualTo(0);
        assertThat(mixed0.getAlpha()).isEqualTo(0);

        RGBColor mixed1 = (RGBColor) ColorValueUtil.mix(c1, c2, 1);
        assertThat(mixed1.getRed()).isEqualTo(255);
        assertThat(mixed1.getGreen()).isEqualTo(255);
        assertThat(mixed1.getBlue()).isEqualTo(255);
        assertThat(mixed1.getAlpha()).isEqualTo(255);

        RGBColor mixedHalf = (RGBColor) ColorValueUtil.mix(c1, c2, 0.5);
        assertThat(mixedHalf.getRed()).isEqualTo(128);
        assertThat(mixedHalf.getGreen()).isEqualTo(128);
        assertThat(mixedHalf.getBlue()).isEqualTo(128);
        assertThat(mixedHalf.getAlpha()).isEqualTo(128);

        RGBColor mixedClamped0 = (RGBColor) ColorValueUtil.mix(c1, c2, -1);
        assertThat(mixedClamped0).isEqualTo(mixed0);

        RGBColor mixedClamped1 = (RGBColor) ColorValueUtil.mix(c1, c2, 2);
        assertThat(mixedClamped1).isEqualTo(mixed1);
    }
}