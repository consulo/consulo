// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.navigationBar.model.NavBarItemPresentationData;
import consulo.navigationBar.model.NavBarPopupItem;
import consulo.navigationBar.model.NavBarVmItem;

public final class DefaultNavBarPopupItem implements NavBarPopupItem {
    private final NavBarVmItem myItem;

    public DefaultNavBarPopupItem(NavBarVmItem item) {
        myItem = item;
    }

    public NavBarVmItem getItem() {
        return myItem;
    }

    @Override
    public NavBarItemPresentationData getPresentation() {
        return (NavBarItemPresentationData) myItem.getPresentation();
    }
}
