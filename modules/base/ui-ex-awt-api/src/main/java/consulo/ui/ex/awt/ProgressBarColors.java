/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.application.ui.UISettings;
import consulo.ui.ex.JBColor;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.function.Supplier;

public final class ProgressBarColors {
    public static final Color GREEN = new JBColor(new Supplier<>() {
        @Nonnull
        @Override
        public Color get() {
            UISettings settings = UISettings.getInstanceOrNull();
            if (settings == null || null == settings.COLOR_BLINDNESS) {
                return new JBColor(new Color(0x6cad74), new Color(0x4a8c53));
            }
            else {
                return new JBColor(new Color(0x6ca69c), new Color(0x639990));
            }
        }
    });

    public static final Color RED = new JBColor(new Supplier<>() {
        @Nonnull
        @Override
        public Color get() {
            UISettings settings = UISettings.getInstanceOrNull();
            if (settings == null || null == settings.COLOR_BLINDNESS) {
                return new JBColor(new Color(0xd67b76), new Color(0xe55757));
            }
            else {
                return new JBColor(new Color(0xcc7447), new Color(0xcc7447));
            }
        }
    });

    public static final Color RED_TEXT = new JBColor(new Color(0xb81708), new Color(0xdb5c5c));

    public static final Color BLUE = new JBColor(new Color(1, 68, 208), JBColor.blue);

    public static final Color YELLOW = new JBColor(new Color(0xa67a21), new Color(0x91703a));

    private ProgressBarColors() {
    }
}
