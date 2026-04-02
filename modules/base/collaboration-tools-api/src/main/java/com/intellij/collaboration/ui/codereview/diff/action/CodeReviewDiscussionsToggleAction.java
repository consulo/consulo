// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.action;

import com.intellij.collaboration.ui.codereview.diff.DiscussionsViewOption;
import com.intellij.collaboration.ui.codereview.diff.model.CodeReviewDiscussionsViewModel;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class CodeReviewDiscussionsToggleAction extends ActionGroup implements DumbAware {
    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        CodeReviewDiscussionsViewModel vm = findViewModel(e.getDataContext());
        e.getPresentation().setEnabledAndVisible(vm != null);
    }

    @Override
    public AnAction @Nonnull [] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return EMPTY_ARRAY;
        }
        CodeReviewDiscussionsViewModel vm = findViewModel(e.getDataContext());
        if (vm == null) {
            return EMPTY_ARRAY;
        }
        DiscussionsViewOption[] options = DiscussionsViewOption.values();
        AnAction[] actions = new AnAction[options.length];
        for (int i = 0; i < options.length; i++) {
            actions[i] = new ToggleOptionAction(vm, options[i]);
        }
        return actions;
    }

    protected abstract @Nullable CodeReviewDiscussionsViewModel findViewModel(@Nonnull DataContext ctx);

    static final class ToggleOptionAction extends ToggleAction {
        private final @Nonnull CodeReviewDiscussionsViewModel vm;
        private final @Nonnull DiscussionsViewOption viewOption;

        ToggleOptionAction(@Nonnull CodeReviewDiscussionsViewModel vm, @Nonnull DiscussionsViewOption viewOption) {
            super(viewOption.toActionName());
            this.vm = vm;
            this.viewOption = viewOption;
        }

        @Override
        public @Nonnull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return vm.getDiscussionsViewOption().getValue() == viewOption;
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            vm.setDiscussionsViewOption(viewOption);
        }
    }
}
