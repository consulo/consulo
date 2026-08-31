/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 *
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
package consulo.desktop.awt.navbar.ui;

import consulo.application.ui.UISettings;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.RelativeFont;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

/**
 * Navigation bar look constants: colors and insets are resolved via the {@code StatusBar.Breadcrumbs.*}
 * LaF keys, with fallbacks for themes that do not define them.
 */
public final class NavBarUi {
    public static final long DEFAULT_UI_RESPONSE_TIMEOUT = 300;

    private NavBarUi() {
    }

    public static Color navBarItemBackground(boolean selected, boolean focused) {
        if (selected && focused) {
            return UIUtil.getListSelectionBackground(true);
        }
        else {
            return UIUtil.getListBackground();
        }
    }

    public static @Nullable Color navBarItemForeground(boolean selected, boolean focused, boolean inactive) {
        if (selected && focused) {
            return UIUtil.getListSelectionForeground(true);
        }
        else if (inactive) {
            return UIUtil.getInactiveTextColor();
        }
        else {
            return null;
        }
    }

    public static @Nullable Font navBarItemFont() {
        if (UISettings.getInstance().getUseSmallLabelsOnTabs()) {
            return RelativeFont.SMALL.derive(UIUtil.getLabelFont());
        }
        return UIUtil.getLabelFont();
    }

    public static Insets navBarItemInsets() {
        return JBUI.insets("StatusBar.Breadcrumbs.itemBackgroundInsets", JBUI.insets(2, 4));
    }

    public static Insets navBarPopupItemInsets() {
        return JBInsets.create(1, 2);
    }

    public static Insets navBarItemPadding(boolean floating) {
        if (floating) {
            return JBUI.insets("StatusBar.Breadcrumbs.floatingItemInsets", JBUI.insets(1));
        }
        else {
            return JBUI.insets("StatusBar.Breadcrumbs.itemInsets", JBUI.insets(2, 0));
        }
    }

    public static int navBarPopupOffset(boolean firstItem) {
        return firstItem ? 0 : JBUI.scale(5);
    }

    // StatusBar.Breadcrumbs.* theme colors

    public static Color foreground() {
        return JBColor.namedColor("StatusBar.Breadcrumbs.foreground", UIUtil.getLabelForeground());
    }

    public static Color hoverForeground() {
        return JBColor.namedColor("StatusBar.Breadcrumbs.hoverForeground", UIUtil.getLabelForeground());
    }

    public static Color hoverBackground() {
        return JBColor.namedColor("StatusBar.Breadcrumbs.hoverBackground", JBCurrentTheme.ActionButton.hoverBackground());
    }

    public static Color selectionForeground() {
        return JBColor.namedColor("StatusBar.Breadcrumbs.selectionForeground", UIUtil.getListSelectionForeground(true));
    }

    public static Color selectionBackground() {
        return JBColor.namedColor("StatusBar.Breadcrumbs.selectionBackground", UIUtil.getListSelectionBackground(true));
    }

    public static Color selectionInactiveForeground() {
        return JBColor.namedColor("StatusBar.Breadcrumbs.selectionInactiveForeground", UIUtil.getListSelectionForeground(false));
    }

    public static Color selectionInactiveBackground() {
        return JBColor.namedColor("StatusBar.Breadcrumbs.selectionInactiveBackground", UIUtil.getListSelectionBackground(false));
    }

    public static Color floatingBackground() {
        return JBColor.namedColor("StatusBar.Breadcrumbs.floatingBackground", UIUtil.getListBackground());
    }

    public static Color floatingForeground() {
        return JBColor.namedColor("StatusBar.Breadcrumbs.floatingForeground", UIUtil.getLabelForeground());
    }

    public static int chevronInset() {
        return JBUI.getInt("StatusBar.Breadcrumbs.chevronInset", 0);
    }

    public static int firstElementLeftOffset() {
        return JBUI.scale(6);
    }
}
