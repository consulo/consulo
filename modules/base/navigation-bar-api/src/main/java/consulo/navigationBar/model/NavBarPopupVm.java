// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.model;

import consulo.navigationBar.model.NavBarPopupItem;

import java.util.List;

/**
 * @param <T> type of item, only required to have a presentation
 */
public interface NavBarPopupVm<T extends NavBarPopupItem> {
    List<T> getItems();

    int getInitialSelectedItemIndex();

    void itemsSelected(List<? extends T> selectedItems);

    void cancel();

    void complete();
}
