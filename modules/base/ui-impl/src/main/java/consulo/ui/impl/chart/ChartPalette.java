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
package consulo.ui.impl.chart;

import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.util.LightDarkColorValue;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ChartPalette {
    private static final List<ColorValue> SERIES = List.of(
        of(0x6BAED6, 0x5B9BD5),
        of(0x62C88E, 0x4CAF7D),
        of(0xF5A65B, 0xD98E45),
        of(0x9E9AC8, 0x8E89BD),
        of(0xE57373, 0xC85C5C),
        of(0x969696, 0x8A8A8A)
    );

    private static final List<ColorValue> FLAME = List.of(
        of(0xF5A65B, 0xB9773A),
        of(0xF7C46C, 0xB8913F),
        of(0xEE8A5A, 0xB06138),
        of(0xF4B183, 0xB5805A),
        of(0xE9C46A, 0xA88C45),
        of(0xF29E7B, 0xB06F55)
    );

    private ChartPalette() {
    }

    public static ColorValue series(int index) {
        return SERIES.get(Math.floorMod(index, SERIES.size()));
    }

    public static ColorValue flame(String name) {
        return FLAME.get(Math.floorMod(name.hashCode(), FLAME.size()));
    }

    private static ColorValue of(int light, int dark) {
        return new LightDarkColorValue(RGBColor.fromRGBValue(light), RGBColor.fromRGBValue(dark));
    }
}
