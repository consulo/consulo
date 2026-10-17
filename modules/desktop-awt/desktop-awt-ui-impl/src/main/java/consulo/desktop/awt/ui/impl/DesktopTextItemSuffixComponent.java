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
package consulo.desktop.awt.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
class DesktopTextItemSuffixComponent extends SimpleColoredComponent {
    DesktopTextItemSuffixComponent() {
        this(JBUI.insets(0, 8, 0, 6));
    }

    DesktopTextItemSuffixComponent(Insets ipad) {
        setOpaque(false);
        setIconOpaque(false);
        setIconOnTheRight(true);
        setIpad(ipad);
    }

    boolean update(LocalizeValue text, @Nullable Image icon, @Nullable Color selectionForeground, @Nullable Font font) {
        clear();
        setFont(font);
        setIcon(icon);

        if (text.isNotEmpty()) {
            append(
                text,
                selectionForeground == null
                    ? SimpleTextAttributes.GRAYED_ATTRIBUTES
                    : new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, selectionForeground)
            );
        }

        boolean visible = text.isNotEmpty() || icon != null;
        setVisible(visible);
        return visible;
    }
}
