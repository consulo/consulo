/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.continuousIntegration.impl.internal.action;

import consulo.application.dumb.DumbAware;
import consulo.continuousIntegration.CIBuildStatus;
import consulo.continuousIntegration.impl.internal.toolWindow.CIToolWindowPanel;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

/**
 * Shows or hides the jobs with one status in the CI tool window.
 *
 * @since 2026-11-14
 */
public class CIStatusFilterAction extends ToggleAction implements DumbAware {
    private final CIBuildStatus myStatus;

    public CIStatusFilterAction(CIBuildStatus status) {
        super(status.getPresentableName(), LocalizeValue.empty(), null);
        myStatus = status;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        CIToolWindowPanel panel = e.getData(CIToolWindowPanel.KEY);
        return panel == null || !panel.getSettings().isHidden(myStatus);
    }

    @RequiredUIAccess
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        CIToolWindowPanel panel = e.getData(CIToolWindowPanel.KEY);
        if (panel != null) {
            panel.getSettings().setHidden(myStatus, !state);
            panel.filterChanged();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(e.getData(CIToolWindowPanel.KEY) != null);
    }
}
