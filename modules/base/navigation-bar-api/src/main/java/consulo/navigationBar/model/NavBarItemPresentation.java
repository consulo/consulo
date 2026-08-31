// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.model;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public interface NavBarItemPresentation {
    /**
     * @param text Text to be shown in the navigation bar.
     */
    static NavBarItemPresentation of(String text) {
        return of(null, text, text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    /**
     * @param text Text to be shown in the navigation bar.
     */
    static NavBarItemPresentation of(@Nullable Image icon, String text) {
        return of(icon, text, text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    /**
     * @param text           Text to be shown in the navigation bar.
     * @param popupText      Text to be shown in the child item popup.
     * @param textAttributes attributes for text and popup text. {@link SimpleTextAttributes#REGULAR_ATTRIBUTES} is used if {@code textAttributes} is {@code null}.
     */
    static NavBarItemPresentation of(
        @Nullable Image icon,
        String text,
        @Nullable String popupText,
        SimpleTextAttributes textAttributes
    ) {
        return new NavBarItemPresentationData(icon, text, popupText, textAttributes, false, false);
    }
}
