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
package consulo.project.ui.impl.internal.wm;

import consulo.project.ui.impl.internal.wm.action.ActivateToolWindowAction;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.ex.toolWindow.ButtonDisplay;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowSettings;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.ui.ex.toolWindow.WindowInfo;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 2026-11-09
 */
public final class ToolWindowStripePlacement {
    public static final int LARGE_ICON_SIZE = 24;

    private ToolWindowStripePlacement() {
    }

    public static boolean isLargeIcons(ToolWindowSettings settings) {
        return settings.getButtonDisplay() == ButtonDisplay.LARGE_ICON;
    }

    public static ToolWindowAnchor getStripeAnchor(ToolWindowSettings settings, WindowInfo info) {
        ToolWindowAnchor anchor = info.getAnchor();
        if (anchor == ToolWindowAnchor.BOTTOM && isLargeIcons(settings)) {
            return info.isSplit() ? ToolWindowAnchor.RIGHT : ToolWindowAnchor.LEFT;
        }
        return anchor;
    }

    public static boolean isAtStripeEnd(ToolWindowSettings settings, WindowInfo info) {
        if (isLargeIcons(settings)) {
            return info.getAnchor() == ToolWindowAnchor.BOTTOM;
        }
        return info.isSplit();
    }

    public static ToolWindowAnchor getDropAnchor(ToolWindowSettings settings, ToolWindowAnchor stripeAnchor, boolean atEnd) {
        if (atEnd && isLargeIcons(settings) && (stripeAnchor == ToolWindowAnchor.LEFT || stripeAnchor == ToolWindowAnchor.RIGHT)) {
            return ToolWindowAnchor.BOTTOM;
        }
        return stripeAnchor;
    }

    public static boolean isDropSplit(ToolWindowSettings settings, ToolWindowAnchor stripeAnchor, boolean atEnd) {
        if (isLargeIcons(settings)) {
            return atEnd && stripeAnchor == ToolWindowAnchor.RIGHT;
        }
        return atEnd;
    }

    public static Comparator<ToolWindowStripeButton> comparator(ToolWindowSettings settings) {
        return Comparator.<ToolWindowStripeButton, Boolean>comparing(button -> isAtStripeEnd(settings, button.getWindowInfo()))
            .thenComparing(button -> button.getWindowInfo().isSplit())
            .thenComparingInt(button -> getOrder(button.getWindowInfo()));
    }

    private static int getOrder(WindowInfo info) {
        return info instanceof WindowInfoImpl impl ? impl.getOrder() : 0;
    }

    public static String getText(ToolWindowSettings settings, ToolWindow window) {
        String text = window.getDisplayName().get();
        int mnemonic = getMnemonic(settings, window);
        return mnemonic != -1 ? (char) mnemonic + ": " + text : text;
    }

    public static int getMnemonic(ToolWindowSettings settings, ToolWindow window) {
        return settings.isShowMnemonic() ? ActivateToolWindowAction.getMnemonicForToolWindow(window.getId()) : -1;
    }

    public static @Nullable Image getIcon(ToolWindowSettings settings, ToolWindow window) {
        Image icon = window.getIcon();
        if (icon != null && isLargeIcons(settings) && icon.getWidth() != 0) {
            return ImageEffects.resize(icon, LARGE_ICON_SIZE / (float) icon.getWidth());
        }
        return icon;
    }
}
