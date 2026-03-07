// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.toolwindow;

import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.toolWindow.ToolWindow;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import jakarta.annotation.Nonnull;

import java.util.Map;

public final class ToolwindowUtils {
    private ToolwindowUtils() {
    }

    public static void dontHideOnEmptyContent(@Nonnull ToolWindow toolWindow) {
        toolWindow.setToHideOnEmptyContent(false);
        if (toolWindow instanceof ToolWindowEx ex) {
            ex.getEmptyText().setText("");
        }
    }
}
