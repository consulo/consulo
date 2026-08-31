// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
import consulo.ui.util.ColorValueUtil;
import org.jspecify.annotations.Nullable;

public final class UIThemeParser {
    private static final Logger LOG = Logger.getInstance(UIThemeParser.class);

    static boolean isColorLike(String text) {
        return text.length() <= 9 && text.startsWith("#");
    }

    static @Nullable RGBColor parseColorOrNull(String value, @Nullable String key) {
        try {
            return ColorValueUtil.fromHexOrNull(value);
        }
        catch (Exception e) {
            if (key != null) {
                LOG.warn(key + "=" + value + " has # prefix but cannot be parsed as color");
            }
            return null;
        }
    }

    static @Nullable Object parseStringValue(String value, String key) {
        if (isColorLike(value)) {
            RGBColor color = parseColorOrNull(value, null);
            if (color == null) {
                LOG.warn(key + "=" + value + " has # prefix but cannot be parsed as color");
                return value;
            }
            return color;
        }
        return value;
    }
}
