// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.model;

import consulo.util.dataholder.Key;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface NavBarVmItem {
    Key<List<NavBarVmItem>> SELECTED_ITEMS = Key.create("nav.bar.selection");

    @Override
    boolean equals(Object other);

    @Override
    int hashCode();

    NavBarItemPresentation getPresentation();

    /**
     * @return future which completes with the children of this item, or with {@code null} if there are none
     */
    CompletableFuture<List<NavBarVmItem>> children();

    /**
     * @return future which completes with the expand result of this item, or with {@code null} if there are no children
     */
    default CompletableFuture<NavBarItemExpandResult> expand() {
        return children().thenApply(children -> children == null ? null : NavBarItemExpandResult.of(children, false));
    }
}
