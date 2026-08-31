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
package consulo.ui.color;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
public class HSLColorTest {
    private static final float EPS = 0.001f;

    @Test
    void testFromRGBPrimaries() {
        HSLColor red = HSLColor.fromRGB(new RGBColor(255, 0, 0));
        assertThat(red.getHue()).isCloseTo(0f, within());
        assertThat(red.getSaturation()).isCloseTo(1f, within());
        assertThat(red.getLightness()).isCloseTo(0.5f, within());

        HSLColor green = HSLColor.fromRGB(new RGBColor(0, 255, 0));
        assertThat(green.getHue()).isCloseTo(120f, within());
        assertThat(green.getSaturation()).isCloseTo(1f, within());
        assertThat(green.getLightness()).isCloseTo(0.5f, within());

        HSLColor blue = HSLColor.fromRGB(new RGBColor(0, 0, 255));
        assertThat(blue.getHue()).isCloseTo(240f, within());
        assertThat(blue.getSaturation()).isCloseTo(1f, within());
        assertThat(blue.getLightness()).isCloseTo(0.5f, within());

        HSLColor cyan = HSLColor.fromRGB(new RGBColor(0, 255, 255));
        assertThat(cyan.getHue()).isCloseTo(180f, within());

        HSLColor magenta = HSLColor.fromRGB(new RGBColor(255, 0, 255));
        assertThat(magenta.getHue()).isCloseTo(300f, within());

        HSLColor yellow = HSLColor.fromRGB(new RGBColor(255, 255, 0));
        assertThat(yellow.getHue()).isCloseTo(60f, within());
    }

    @Test
    void testFromRGBAchromatic() {
        HSLColor white = HSLColor.fromRGB(new RGBColor(255, 255, 255));
        assertThat(white.getSaturation()).isCloseTo(0f, within());
        assertThat(white.getLightness()).isCloseTo(1f, within());

        HSLColor black = HSLColor.fromRGB(new RGBColor(0, 0, 0));
        assertThat(black.getSaturation()).isCloseTo(0f, within());
        assertThat(black.getLightness()).isCloseTo(0f, within());

        HSLColor gray = HSLColor.fromRGB(new RGBColor(128, 128, 128));
        assertThat(gray.getSaturation()).isCloseTo(0f, within());
        assertThat(gray.getLightness()).isCloseTo(128f / 255f, within());
    }

    @Test
    void testToRGBKnownValues() {
        assertThat(new HSLColor(0f, 1f, 0.5f).toRGB()).isEqualTo(new RGBColor(255, 0, 0));
        assertThat(new HSLColor(120f, 1f, 0.5f).toRGB()).isEqualTo(new RGBColor(0, 255, 0));
        assertThat(new HSLColor(240f, 1f, 0.5f).toRGB()).isEqualTo(new RGBColor(0, 0, 255));
        assertThat(new HSLColor(60f, 1f, 0.5f).toRGB()).isEqualTo(new RGBColor(255, 255, 0));
        assertThat(new HSLColor(0f, 0f, 1f).toRGB()).isEqualTo(new RGBColor(255, 255, 255));
        assertThat(new HSLColor(0f, 0f, 0f).toRGB()).isEqualTo(new RGBColor(0, 0, 0));
        assertThat(new HSLColor(120f, 1f, 0.25f).toRGB()).isEqualTo(new RGBColor(0, 128, 0));
    }

    @Test
    void testRoundTrip() {
        for (int r = 0; r < 256; r += 15) {
            for (int g = 0; g < 256; g += 15) {
                for (int b = 0; b < 256; b += 15) {
                    RGBColor original = new RGBColor(r, g, b);
                    RGBColor restored = HSLColor.fromRGB(original).toRGB();

                    assertThat(restored).as("round trip of %s", original).isEqualTo(original);
                }
            }
        }
    }

    @Test
    void testAlphaPreserved() {
        RGBColor translucent = new RGBColor(10, 20, 30, 40);

        HSLColor hsl = HSLColor.fromRGB(translucent);
        assertThat(hsl.getAlpha()).isEqualTo(40);
        assertThat(hsl.toRGB().getAlpha()).isEqualTo(40);

        ColorValue withAlpha = hsl.withAlpha(200);
        assertThat(withAlpha).isInstanceOf(HSLColor.class);
        assertThat(withAlpha.toRGB().getAlpha()).isEqualTo(200);
    }

    @Test
    void testHueNormalization() {
        RGBColor expected = new HSLColor(10f, 1f, 0.5f).toRGB();

        assertThat(new HSLColor(370f, 1f, 0.5f).toRGB()).isEqualTo(expected);
        assertThat(new HSLColor(-350f, 1f, 0.5f).toRGB()).isEqualTo(expected);
    }

    @Test
    void testToHSL() {
        HSLColor hsl = new HSLColor(42f, 0.5f, 0.5f);
        assertThat(hsl.toHSL()).isSameAs(hsl);

        RGBColor rgb = new RGBColor(255, 0, 0);
        HSLColor converted = rgb.toHSL();
        assertThat(converted.getHue()).isCloseTo(0f, within());
        assertThat(converted.getSaturation()).isCloseTo(1f, within());
        assertThat(converted.getLightness()).isCloseTo(0.5f, within());
    }

    private static Offset<Float> within() {
        return Offset.offset(EPS);
    }

}
