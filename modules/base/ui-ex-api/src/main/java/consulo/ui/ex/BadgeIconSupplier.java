// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.ui.ex;

import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleColorValue;
import consulo.util.lang.lazy.LazyValue;

import java.util.function.Supplier;

/**
 * Provides icon variants with additional badges such as error, warning, information, and success on top of the original icon.
 * It also decorates icons with a live indicator.
 */
public final class BadgeIconSupplier {
    private static final int BADGE_RADIUS = 3;

    private final Image myOriginalIcon;
    private final Supplier<Image> myErrorIcon;
    private final Supplier<Image> myWarningIcon;
    private final Supplier<Image> myInfoIcon;
    private final Supplier<Image> mySuccessIcon;

    /**
     * @param originalIcon the original icon to which badges are to be applied
     */
    public BadgeIconSupplier(Image originalIcon) {
        myOriginalIcon = originalIcon;
        myErrorIcon = LazyValue.notNull(() -> withIconBadge(originalIcon, ComponentColors.ICON_BADGE_ERROR));
        myWarningIcon = LazyValue.notNull(() -> withIconBadge(originalIcon, ComponentColors.ICON_BADGE_WARNING));
        myInfoIcon = LazyValue.notNull(() -> withIconBadge(originalIcon, ComponentColors.ICON_BADGE_INFO));
        mySuccessIcon = LazyValue.notNull(() -> withIconBadge(originalIcon, ComponentColors.ICON_BADGE_SUCCESS));
    }

    public Image getOriginalIcon() {
        return myOriginalIcon;
    }

    public Image getErrorIcon() {
        return myErrorIcon.get();
    }

    public Image getWarningIcon() {
        return myWarningIcon.get();
    }

    public Image getInfoIcon() {
        return myInfoIcon.get();
    }

    public Image getSuccessIcon() {
        return mySuccessIcon.get();
    }

    public Image getLiveIndicatorIcon() {
        return getSuccessIcon();
    }

    public Image getErrorIcon(boolean error) {
        return error ? getErrorIcon() : myOriginalIcon;
    }

    public Image getWarningIcon(boolean warning) {
        return warning ? getWarningIcon() : myOriginalIcon;
    }

    public Image getInfoIcon(boolean info) {
        return info ? getInfoIcon() : myOriginalIcon;
    }

    public Image getSuccessIcon(boolean success) {
        return success ? getSuccessIcon() : myOriginalIcon;
    }

    public Image getLiveIndicatorIcon(boolean alive) {
        return alive ? getLiveIndicatorIcon() : myOriginalIcon;
    }

    /**
     * {@code icon} with a dot of {@code color} in its top right corner.
     */
    public static Image withIconBadge(Image icon, StyleColorValue color) {
        int width = icon.getWidth();
        int height = icon.getHeight();
        return ImageEffects.canvas(width, height, canvas -> {
            canvas.drawImage(icon, 0, 0);
            canvas.setFillStyle(color);
            canvas.beginPath();
            canvas.arc(width - BADGE_RADIUS, BADGE_RADIUS, BADGE_RADIUS, 0, 2 * Math.PI);
            canvas.fill();
        });
    }
}
