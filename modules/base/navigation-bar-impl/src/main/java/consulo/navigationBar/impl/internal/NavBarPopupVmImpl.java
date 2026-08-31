// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.navigationBar.model.NavBarPopupItem;
import consulo.navigationBar.model.NavBarPopupVm;
import consulo.ui.UIAccess;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class NavBarPopupVmImpl<T extends NavBarPopupItem> implements NavBarPopupVm<T> {
    private final List<T> myItems;
    private final int myInitialSelectedItemIndex;

    private List<T> mySelectedItems;

    private final CompletableFuture<T> myResult = new CompletableFuture<>();

    public NavBarPopupVmImpl(List<T> items, int initialSelectedItemIndex) {
        myItems = items;
        myInitialSelectedItemIndex = initialSelectedItemIndex;
        mySelectedItems = initialSelectedItemIndex == -1 ? List.of() : List.of(items.get(initialSelectedItemIndex));
    }

    @Override
    public List<T> getItems() {
        return myItems;
    }

    @Override
    public int getInitialSelectedItemIndex() {
        return myInitialSelectedItemIndex;
    }

    public List<T> getSelectedItems() {
        UIAccess.assertIsUIThread();
        return mySelectedItems;
    }

    @Override
    public void itemsSelected(List<? extends T> selectedItems) {
        UIAccess.assertIsUIThread();
        mySelectedItems = List.copyOf(selectedItems);
    }

    public CompletableFuture<T> getResult() {
        return myResult;
    }

    @Override
    public void cancel() {
        myResult.cancel(false);
    }

    @Override
    public void complete() {
        if (!mySelectedItems.isEmpty()) {
            myResult.complete(mySelectedItems.get(0));
        }
    }
}
