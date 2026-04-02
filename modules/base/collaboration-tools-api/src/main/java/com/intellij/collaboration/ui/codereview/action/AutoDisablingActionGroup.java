// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.action;

import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.util.ActionGroupUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Action group will be disabled if all children are disabled
 */
@ApiStatus.Internal
public final class AutoDisablingActionGroup extends DefaultActionGroup {
    public AutoDisablingActionGroup(@Nullable @NlsActions.ActionText String shortName, boolean popup) {
        super(shortName, popup);
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        var enabledActions = ActionGroupUtil.getActiveActions(this, e);
        e.getPresentation().setEnabled(enabledActions.isNotEmpty());
    }
}
