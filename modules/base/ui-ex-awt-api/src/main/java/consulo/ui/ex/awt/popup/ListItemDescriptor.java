// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.popup;

import consulo.ui.image.Image;
import jakarta.annotation.Nullable;

public interface ListItemDescriptor<T> {
    @Nullable
    String getTextFor(T value);

    @Nullable
    default String getTooltipFor(T value) {
        return null;
    }

    @Nullable
    default Image getIconFor(T value) {
        return null;
    }

    default Image getSelectedIconFor(T value) {
        return getIconFor(value);
    }

    @Deprecated
    default boolean hasSeparatorAboveOf(T value) {
        return false;
    }

    @Nullable
    @Deprecated
    default String getCaptionAboveOf(T value) {
        return null;
    }

    default boolean isSeparator(T value) {
        return false;
    }
}
