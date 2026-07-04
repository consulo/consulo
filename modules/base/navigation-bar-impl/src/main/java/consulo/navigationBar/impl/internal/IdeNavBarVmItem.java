// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.component.util.pointer.Pointer;
import consulo.navigationBar.NavBarItem;
import consulo.navigationBar.model.NavBarItemExpandResult;
import consulo.navigationBar.model.NavBarItemPresentationData;
import consulo.navigationBar.model.NavBarVmItem;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class IdeNavBarVmItem implements NavBarVmItem {
    private final Pointer<? extends NavBarItem> myPointer;
    private final NavBarItemPresentationData myPresentation;

    // Synthetic string field for fast equality heuristics
    // Used to match element's direct child in the navbar with the same child in its popup
    private final String myTexts;

    @RequiredReadAction
    public IdeNavBarVmItem(NavBarItem item) {
        Application.get().assertReadAccessAllowed();
        myPointer = item.createPointer();
        myPresentation = (NavBarItemPresentationData) item.presentation();
        myTexts = item.getClass().getCanonicalName() + "$" +
            myPresentation.text().replace("$", "$$") + "$" +
            (myPresentation.popupText() == null ? "null" : myPresentation.popupText().replace("$", "$$"));
    }

    public Pointer<? extends NavBarItem> getPointer() {
        return myPointer;
    }

    @Override
    public NavBarItemPresentationData getPresentation() {
        return myPresentation;
    }

    @Override
    public CompletableFuture<List<NavBarVmItem>> children() {
        return fetch(IdeNavBarVmItem::childItems);
    }

    @Override
    public CompletableFuture<NavBarItemExpandResult> expand() {
        return fetch(item -> NavBarItemExpandResult.of(childItems(item), item.navigateOnClick()));
    }

    @RequiredReadAction
    private static List<NavBarVmItem> childItems(NavBarItem item) {
        return toVmItems(NavBarItemUtil.children(item));
    }

    @RequiredReadAction
    public static List<NavBarVmItem> toVmItems(List<NavBarItem> items) {
        List<NavBarVmItem> result = new ArrayList<>(items.size());
        for (NavBarItem item : items) {
            result.add(new IdeNavBarVmItem(item));
        }
        return result;
    }

    private <T> CompletableFuture<T> fetch(Function<NavBarItem, T> selector) {
        CoroutineScope scope = new CoroutineScope(Application.get().coroutineContext());
        return Coroutine.first(ReadLock.<@Nullable Object, T>apply(ignored -> {
            NavBarItem item = myPointer.dereference();
            return item == null ? null : selector.apply(item);
        })).runAsync(scope, null).toFuture();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return myTexts.equals(((IdeNavBarVmItem) other).myTexts);
    }

    @Override
    public int hashCode() {
        return myTexts.hashCode();
    }

    @Override
    public String toString() {
        return myTexts;
    }
}
