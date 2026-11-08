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
package consulo.welcomeScreen.impl.internal;

import consulo.ui.color.ColorValue;
import consulo.ui.color.HSLColor;
import consulo.ui.impl.style.IslandsState;
import consulo.ui.internal.GradientColor;
import consulo.ui.internal.GradientColorBlob;
import consulo.ui.style.ComponentColors;

import java.util.ArrayList;
import java.util.List;

/**
 * A soft glow from the middle in the colour of the theme's controls, over the island background (the editor area
 * background without islands). A large spot in the centre and two smaller ones beside it, at fixed places, in
 * neighbouring hues; how strong and how light they are follows the base colour, so the glow reads in a light and in
 * a dark theme, and after a theme switch.
 *
 * @author VISTALL
 * @since 2026-11-07
 */
public final class WelcomeBackdrop {
    private static final float NEIGHBOUR_HUE_SHIFT = 28f;

    private WelcomeBackdrop() {
    }

    public static ColorValue baseColor() {
        return ColorValue.lazy(() -> IslandsState.isEnabled()
            ? ComponentColors.TOOL_WINDOW_BACKGROUND
            : ComponentColors.EDITOR_AREA_BACKGROUND);
    }

    public static GradientColor create() {
        ColorValue base = baseColor();
        ColorValue accent = ComponentColors.FOCUS_COLOR;

        List<GradientColorBlob> blobs = new ArrayList<>(3);
        blobs.add(new GradientColorBlob(0.5f, 0.5f, 0.6f, ColorValue.lazy(() -> glowColor(base, accent, 0f, 1f))));
        blobs.add(new GradientColorBlob(0.4f, 0.42f, 0.35f, ColorValue.lazy(() -> glowColor(base, accent, -NEIGHBOUR_HUE_SHIFT, 0.8f))));
        blobs.add(new GradientColorBlob(0.6f, 0.58f, 0.35f, ColorValue.lazy(() -> glowColor(base, accent, NEIGHBOUR_HUE_SHIFT, 0.8f))));
        return new GradientColor(base, blobs);
    }

    private static ColorValue glowColor(ColorValue base, ColorValue accent, float hueShift, float strength) {
        float hue = (HSLColor.fromRGB(accent.toRGB()).getHue() + hueShift + 360f) % 360f;
        boolean dark = HSLColor.fromRGB(base.toRGB()).getLightness() < 0.5f;
        if (dark) {
            return new HSLColor(hue, 0.6f, 0.45f, Math.round(110 * strength));
        }
        return new HSLColor(hue, 0.8f, 0.7f, Math.round(150 * strength));
    }
}
