// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.model;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Internal: plugins are not supposed to read the presentation, they are only supposed to provide it.
 *
 * @param hasContainingFile   if {@code true}, {@code icon} will be displayed, otherwise {@code false} the icon will be displayed
 *                            if the item is the last item.
 * @param isModuleContentRoot if {@code true}, a special icon will be displayed regardless of {@code icon} and {@code hasContainingFile}.
 */
public record NavBarItemPresentationData(
    @Nullable Image icon,
    String text,
    @Nullable String popupText,
    @Nullable SimpleTextAttributes textAttributes,
    boolean hasContainingFile,
    boolean isModuleContentRoot
) implements NavBarItemPresentation {

    /**
     * Sets whether the item originates from a source file.
     * If {@code true}, {@code icon} will be displayed, otherwise, {@code icon} will be displayed if the item is the last item in the bar.
     * <p>
     * The platform already handles PSI-based navigation items
     * => plugins are not expected to provide items (and presentation) from PSI
     * => this flag is not exposed for third-party usage.
     * <p>
     * TODO Find a better way to support this
     */
    public NavBarItemPresentation hasContainingFile(boolean hasContainingFile) {
        return new NavBarItemPresentationData(icon, text, popupText, textAttributes, hasContainingFile, isModuleContentRoot);
    }
}
