// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.items;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;

import java.awt.*;

/**
 * {@link RunAnythingItem} represents an item of 'Run Anything' list
 */
public abstract class RunAnythingItem {
    /**
     * Returns text presentation of command
     */
    @Nonnull
    public abstract String getCommand();

    /**
     * Creates current item {@link Component}
     *
     * @param isSelected true if item is selected in the list
     * @deprecated use {@link #createComponent(String, Icon, boolean, boolean)}
     */
    @Deprecated
    @Nullable
    public Component createComponent(boolean isSelected) {
        return null;
    }

    /**
     * Creates current item {@link Component}
     *
     * @param pattern    search field input field
     * @param isSelected true if item is selected in the list
     * @param hasFocus   true if item has focus in the list
     */
    @Nonnull
    public abstract Component createComponent(@Nullable String pattern, boolean isSelected, boolean hasFocus);
}
