// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.ui.layout.action;

import consulo.execution.icon.ExecutionIconGroup;
import consulo.execution.internal.layout.RunnerContentUi;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import org.jspecify.annotations.Nullable;

/**
 * @author Jeka
 */
public final class RestoreLayoutAction extends DumbAwareAction {
    public RestoreLayoutAction() {
        super(
            ActionLocalize.actionRunnerRestorelayoutText(),
            ActionLocalize.actionRunnerRestorelayoutDescription(),
            ExecutionIconGroup.actionRestorelayout()
        );
    }

    public static @Nullable RunnerContentUi getRunnerUi(AnActionEvent e) {
        return e.getData(RunnerContentUi.KEY);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        RunnerContentUi ui = e.getRequiredData(RunnerContentUi.KEY);
        ui.restoreLayout();
    }

    @Override
    public void update(AnActionEvent e) {
        RunnerContentUi runnerContentUi = e.getData(RunnerContentUi.KEY);
        boolean enabled = false;
        if (runnerContentUi != null) {
            enabled = true;
            if (ActionPlaces.DEBUGGER_TOOLBAR.equals(e.getPlace())) {
                // In this case the action has to available in ActionPlaces.RUNNER_LAYOUT_BUTTON_TOOLBAR only
                enabled = !runnerContentUi.isMinimizeActionEnabled();
            }
        }
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}