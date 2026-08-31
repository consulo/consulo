// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.ui.impl.style;

import consulo.logging.Logger;
import consulo.ui.color.RGBColor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorMap {
    private static final Logger LOG = Logger.getInstance(ColorMap.class);

    private static final RGBColor TRANSPARENT = new RGBColor(0, 0, 0, 0);

    public Map<String, RGBColor> map = Map.of();

    public @Nullable Map<String, ThemeColorValue> rawMap;

    public sealed interface ThemeColorValue permits NamedColorValue, RGBColorValue {
    }

    public record NamedColorValue(String name) implements ThemeColorValue {
    }

    public record RGBColorValue(RGBColor color) implements ThemeColorValue {
    }

    static void initializeNamedColors(UIThemeBean theme) {
        Map<String, ThemeColorValue> rawColorMap = theme.colorMap.rawMap;
        if (rawColorMap == null || rawColorMap.isEmpty()) {
            theme.colorMap.map = Map.of();
            return;
        }

        List<String[]> namedNamedValues = new ArrayList<>();

        Map<String, RGBColor> colorMap = new HashMap<>(rawColorMap.size());
        theme.colorMap.map = colorMap;
        for (Map.Entry<String, ThemeColorValue> entry : rawColorMap.entrySet()) {
            String key = entry.getKey();
            ThemeColorValue value = entry.getValue();
            if (value instanceof RGBColorValue rgbColorValue) {
                colorMap.put(key, rgbColorValue.color());
                continue;
            }

            String colorName = ((NamedColorValue)value).name();
            ThemeColorValue color = rawColorMap.get(colorName);
            if (color == null) {
                LOG.warn("Color " + colorName + " is not mapped for key " + key);
                colorMap.put(key, TRANSPARENT);
            }
            else if (color instanceof RGBColorValue rgbColorValue) {
                colorMap.put(key, rgbColorValue.color());
            }
            else {
                NamedColorValue namedColorValue = (NamedColorValue)color;
                if (colorName.equals(namedColorValue.name())) {
                    LOG.warn("Can't handle value " + color + " for key '" + key + "'");
                }
                else {
                    namedNamedValues.add(new String[]{key, namedColorValue.name()});
                }
            }
        }

        for (String[] colorInfo : namedNamedValues) {
            RGBColor color = colorMap.get(colorInfo[1]);
            if (color != null) {
                colorMap.put(colorInfo[0], color);
            }
            else {
                LOG.warn("Can't handle value " + colorInfo[1] + " for key '" + colorInfo[0] + "'");
            }
        }
    }
}
