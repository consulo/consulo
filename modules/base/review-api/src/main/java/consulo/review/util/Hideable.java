// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.util;

import consulo.util.concurrent.coroutine.ObservableValue;

public interface Hideable {
    /**
     * If there's at least one hidden item, hides them all, otherwise shows all items
     */
    static void syncOrToggleAll(Iterable<? extends Hideable> items) {
        boolean noneShowing = true;
        for (Hideable item : items) {
            if (!item.getHiddenState().get()) {
                noneShowing = false;
                break;
            }
        }
        for (Hideable item : items) {
            item.setHidden(!noneShowing);
        }
    }

    ObservableValue<Boolean> getHiddenState();

    void setHidden(boolean hidden);
}
