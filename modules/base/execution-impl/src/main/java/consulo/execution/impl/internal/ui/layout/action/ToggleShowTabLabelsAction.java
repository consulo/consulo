// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.ui.layout.action;

import consulo.execution.internal.layout.RunnerContentUi;
import consulo.execution.localize.ExecutionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;

import java.util.Objects;

public final class ToggleShowTabLabelsAction extends DumbAwareToggleAction {
    public ToggleShowTabLabelsAction() {
        super(ExecutionLocalize.actionRunnerToggletablabelsText());
    }
    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(!ActionPlaces.DEBUGGER_TOOLBAR.equals(e.getPlace()) && e.hasData(RunnerContentUi.KEY));
        super.update(e);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        var runnerUI = e.getData(RunnerContentUi.KEY);
        return runnerUI != null && !runnerUI.getLayoutSettings().isTabLabelsHidden();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean state) {
        RunnerContentUi runnerUI = e.getRequiredData(RunnerContentUi.KEY);
        runnerUI.getLayoutSettings().setTabLabelsHidden(!state);
        runnerUI.updateTabsUI(true);
    }
}
