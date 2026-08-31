// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.util.pointer.Pointer;
import consulo.navigation.Navigatable;
import consulo.navigationBar.model.NavBarItemPresentation;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * An abstraction to be presented within a navigation bar.
 */
public interface NavBarItem {
    Key<Pointer<? extends NavBarItem>> NAVBAR_ITEM_KEY = Key.create("navigationBarItem");

    /**
     * Creates a pointer to weakly store this item in UI components.
     */
    @RequiredReadAction
    Pointer<? extends NavBarItem> createPointer();

    /**
     * Precalculates the presentation aspects of this item.
     *
     * @see NavBarItemPresentation
     */
    @RequiredReadAction
    NavBarItemPresentation presentation();

    /**
     * Returns a {@link Navigatable} for this item if it represents a navigatable
     * entity and {@code null} otherwise.
     *
     * @see consulo.navigation.Navigatable
     */
    @RequiredReadAction
    default @Nullable Navigatable navigationRequest() {
        return null;
    }

    /**
     * Indicates whether navigation should be performed when item is selected from the popup menu.
     * The default behaviour is {@code false}, i.e. to show next popup with selected item's children.
     */
    default boolean navigateOnClick() {
        return false;
    }

    /**
     * Returns weight for this item for sorting when it is presented in navbar children popup
     */
    default int weight() {
        return Integer.MAX_VALUE;
    }
}
