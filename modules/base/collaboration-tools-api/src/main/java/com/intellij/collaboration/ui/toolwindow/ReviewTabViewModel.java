// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.toolwindow;

import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

/**
 * View model of the review toolwindow tab content
 */
//TODO: move name and description to component factory and potentially remove this class
public interface ReviewTabViewModel {
    /**
     * Toolwindow tab title
     */
    @Nls
    @Nonnull
    String getDisplayName();

    /**
     * Toolwindow tab tooltip
     */
    default @NlsContexts.Tooltip @Nonnull String getDescription() {
        return getDisplayName();
    }
}
