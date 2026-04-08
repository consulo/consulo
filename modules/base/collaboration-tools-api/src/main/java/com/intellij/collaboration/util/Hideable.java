// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ApiStatus.Experimental
public interface Hideable {
    @Nonnull
    StateFlow<Boolean> getHiddenState();

    void setHidden(boolean hidden);

    /**
     * If there's at least one hidden item, hides them all, otherwise shows all items
     */
    static void syncOrToggleAll(@Nonnull Iterator<? extends Hideable> items) {
        List<Hideable> list = new ArrayList<>();
        items.forEachRemaining(list::add);

        boolean noneShowing = list.stream().noneMatch(h -> !((Boolean) h.getHiddenState().getValue()));
        if (noneShowing) {
            for (Hideable h : list) {
                h.setHidden(false);
            }
        }
        else {
            for (Hideable h : list) {
                h.setHidden(true);
            }
        }
    }
}
