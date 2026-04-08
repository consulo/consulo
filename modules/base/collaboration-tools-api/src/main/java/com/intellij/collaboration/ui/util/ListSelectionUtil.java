// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import consulo.application.util.ListSelection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class ListSelectionUtil {
    private ListSelectionUtil() {
    }

    public static <T> @Nullable T getSelectedItem(@Nonnull ListSelection<T> selection) {
        var list = selection.getList();
        int index = selection.getSelectedIndex();
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }
}
