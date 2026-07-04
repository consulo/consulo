// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.model;

import java.util.List;

public interface NavBarItemExpandResult {
    /**
     * <h3>Child popup</h3>
     * <p>
     * When the current item has siblings, it's displayed in the popup with its siblings.
     * Selecting the item triggers the following:
     * <ul>
     * <li>if {@code navigateOnClick} is {@code true}, the navigation to the current item will be attempted.</li>
     * <li>otherwise, that the next popup will be shown with {@code children} in its content.</li>
     * </ul>
     * If there are no children, the navigation is attempted on the current item regardless of {@code navigateOnClick}.
     *
     * <h3>Auto-expand</h3>
     * <p>
     * When the current item is a single child itself,
     * it may be automatically "selected" in the popup, and the popup will be effectively skipped.
     * This is done in a loop, which ends when some item returns multiple children.
     * <p>
     * Items may be navigatable, and we want to give the user an opportunity to navigate to the item,
     * even if the item is a single child and thus is about to be skipped.
     * The expanding loop will stop once it encounters an item with {@code navigateOnClick}
     * regardless whether it's a single child or not, which results in a popup with a single element.
     *
     * @param children        children of the current item
     * @param navigateOnClick whether the current item should be navigatable when selected in the child popup
     */
    static NavBarItemExpandResult of(List<NavBarVmItem> children, boolean navigateOnClick) {
        return new NavBarItemExpandResultData(children, navigateOnClick);
    }
}
