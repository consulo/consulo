// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.model;

public interface NavBarItemVm {
    NavBarItemPresentationData getPresentation();

    boolean isFirst();

    boolean isLast();

    boolean isSelected();

    boolean isNextSelected();

    boolean isInactive();

    void select();

    void showPopup();

    void activate();
}
