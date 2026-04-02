// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.action;

import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeListViewModel;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

abstract sealed class CodeReviewShowDiffActionProvider implements AnActionExtensionProvider
    permits CodeReviewShowDiffActionProvider.Preview {

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public boolean isActive(@Nonnull AnActionEvent e) {
        return e.getData(CodeReviewChangeListViewModel.DATA_KEY) != null;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        CodeReviewChangeListViewModel vm = e.getData(CodeReviewChangeListViewModel.DATA_KEY);
        e.getPresentation().setEnabled(vm != null && vm.getChangesSelection().getValue() != null);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        CodeReviewChangeListViewModel vm = e.getData(CodeReviewChangeListViewModel.DATA_KEY);
        if (vm == null) {
            return;
        }
        doShowDiff(vm);
    }

    protected abstract void doShowDiff(@Nonnull CodeReviewChangeListViewModel vm);

    static final class Preview extends CodeReviewShowDiffActionProvider {
        @Override
        protected void doShowDiff(@Nonnull CodeReviewChangeListViewModel vm) {
            vm.showDiffPreview();
        }
    }

  /*static final class Standalone extends CodeReviewShowDiffActionProvider {
    @Override
    protected void doShowDiff(@Nonnull CodeReviewChangeListViewModel vm) {
      vm.showDiff();
    }
  }*/
}
