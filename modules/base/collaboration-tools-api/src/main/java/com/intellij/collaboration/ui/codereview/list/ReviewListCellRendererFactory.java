// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list;

import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.function.Function;

@ApiStatus.Internal
public final class ReviewListCellRendererFactory {
    private ReviewListCellRendererFactory() {
    }

    public static <T> @Nonnull ListCellRenderer<T> getCellRenderer(@Nonnull Function<T, ReviewListItemPresentation> presenter) {
        return new ReviewListCellRenderer<>(presenter);
    }
}
