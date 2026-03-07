// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.action;

import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.awt.action.ToolbarLabelAction;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

public final class ImmutableToolbarLabelAction extends ToolbarLabelAction {
    public ImmutableToolbarLabelAction(@Nls @Nonnull String text) {
        getTemplatePresentation().setText(text);
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
