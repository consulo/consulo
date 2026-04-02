// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.icon;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @param <T> icon key type
 */
@FunctionalInterface
public interface IconsProvider<T> {

    /**
     * @param key      icon key
     * @param iconSize required icon size in pixels (unscaled)
     */
    @RequiresEdt
    @Nonnull
    Icon getIcon(@Nullable T key, int iconSize);
}
