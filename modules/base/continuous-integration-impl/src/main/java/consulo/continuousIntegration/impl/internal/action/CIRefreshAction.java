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

import consulo.annotation.component.ActionImpl;
import consulo.continuousIntegration.impl.internal.toolWindow.CIToolWindowPanel;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;

/**
 * @since 2026-11-09
 */
@ActionImpl(id = "ContinuousIntegration.Refresh")
public class CIRefreshAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public CIRefreshAction() {
        super(
            ContinuousIntegrationImplLocalize.actionRefreshText(),
            ContinuousIntegrationImplLocalize.actionRefreshDescription(),
            PlatformIconGroup.actionsRefresh()
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        CIToolWindowPanel panel = e.getData(CIToolWindowPanel.KEY);
        if (panel != null) {
            panel.refresh();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getData(CIToolWindowPanel.KEY) != null);
    }
}
