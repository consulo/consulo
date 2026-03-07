// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth.ui;

import com.intellij.collaboration.auth.Account;
import consulo.ui.ex.RelativePoint;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JComponent;

/**
 * Allows passing UI action implementation to {@link AccountsPanelFactory} from outside
 */
public interface AccountsPanelActionsController<A extends Account> {
    /**
     * {@code true} if {@link #addAccount} shows a popup with sub-actions, {@code false} otherwise
     */
    boolean isAddActionWithPopup();

    void addAccount(@Nonnull JComponent parentComponent, @Nullable RelativePoint point);

    default void addAccount(@Nonnull JComponent parentComponent) {
        addAccount(parentComponent, null);
    }

    void editAccount(@Nonnull JComponent parentComponent, @Nonnull A account);
}
