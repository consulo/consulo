// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search;

import consulo.application.dumb.DumbAware;
import consulo.fileEditor.localize.FileEditorLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

public final class StopRangeSearchAction extends AnAction implements DumbAware {

    private final RangeSearch myRangeSearch;

    public StopRangeSearchAction(@Nonnull RangeSearch rangeSearch) {
        this.myRangeSearch = rangeSearch;
        getTemplatePresentation().setTextValue(FileEditorLocalize.largeFileEditorStopSearchingActionText());
        getTemplatePresentation().setIcon(PlatformIconGroup.actionsSuspend());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        RangeSearchTask task = myRangeSearch.getLastExecutedRangeSearchTask();
        e.getPresentation().setEnabled(
            task != null && !task.isFinished() && !task.isShouldStop()
        );
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        RangeSearchTask task = myRangeSearch.getLastExecutedRangeSearchTask();
        if (task != null) {
            task.shouldStop();
        }
    }
}