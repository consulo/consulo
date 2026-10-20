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
package consulo.ui.impl;

import consulo.ui.PopupStyle;
import consulo.ui.ToastStyle;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

/**
 * The {@link PopupStyle} a light popup was given, shared by every frontend. The colours are the ones of a toast of
 * the same style, see {@link ToastPresentation}.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
public final class PopupStyleState {
    private @Nullable ToastStyle mySeverity;
    private boolean myGlassy;

    public void add(PopupStyle style) {
        switch (style) {
            case INFO -> mySeverity = ToastStyle.INFO;
            case SUCCESS -> mySeverity = ToastStyle.SUCCESS;
            case WARNING -> mySeverity = ToastStyle.WARNING;
            case ERROR -> mySeverity = ToastStyle.ERROR;
            case GLASSY -> myGlassy = true;
        }
    }

    public boolean isGlassy() {
        return myGlassy;
    }

    public boolean hasSeverity() {
        return mySeverity != null;
    }

    /**
     * The fill of the popup - {@code explicit} is what was set as its background colour, which a severity wins over.
     * {@code null} when the popup is to be drawn in its default fill.
     */
    public @Nullable ColorValue background(@Nullable ColorValue explicit) {
        ToastStyle severity = mySeverity;
        return severity != null ? ToastPresentation.background(severity) : explicit;
    }

    /**
     * The border of the popup, {@code null} when the popup is to be drawn with the border it would have without a
     * style.
     */
    public @Nullable ColorValue border() {
        ToastStyle severity = mySeverity;
        return severity != null ? ToastPresentation.border(severity) : null;
    }
}
