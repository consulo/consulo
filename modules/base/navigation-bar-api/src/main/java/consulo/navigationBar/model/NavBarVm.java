// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.model;

import org.jspecify.annotations.Nullable;

import java.util.List;

public interface NavBarVm {
    List<? extends NavBarItemVm> getItems();

    int getSelectedIndex();

    @Nullable NavBarPopupVm<?> getPopup();

    List<NavBarVmItem> selection();

    enum SelectionShift {
        FIRST,
        PREV,
        NEXT,
        LAST,
    }

    void shiftSelectionTo(SelectionShift shift);

    void selectTail(boolean withPopupOpen);

    void showPopup();

    void addListener(NavBarVmListener listener);

    void removeListener(NavBarVmListener listener);
}
