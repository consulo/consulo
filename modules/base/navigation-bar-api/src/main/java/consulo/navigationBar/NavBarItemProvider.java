// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Extension point to fill navigation bar data structure. Connects an item with its parent and children.
 *
 * To find the current focused element provide a data rule for {@link NavBarItem#NAVBAR_ITEM_KEY}.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface NavBarItemProvider {
    /**
     * Finds a known parent of this item if any.
     */
    @RequiredReadAction
    default @Nullable NavBarItem findParent(NavBarItem item) {
        return null;
    }

    /**
     * Lists known item's children if any.
     */
    @RequiredReadAction
    default Iterable<NavBarItem> iterateChildren(NavBarItem item) {
        return List.of();
    }
}
