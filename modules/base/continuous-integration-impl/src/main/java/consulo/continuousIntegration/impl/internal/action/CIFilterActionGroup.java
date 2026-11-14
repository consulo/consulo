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
import consulo.application.dumb.DumbAware;
import consulo.continuousIntegration.CIBuildStatus;
import consulo.continuousIntegration.impl.internal.toolWindow.CIToolWindowPanel;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * Popup of the tool window toolbar choosing the statuses of the jobs shown in the tree; filtering happens in the tool
 * window only, the servers are always asked for all their jobs.
 *
 * @since 2026-11-14
 */
@ActionImpl(id = "ContinuousIntegration.Filter")
public class CIFilterActionGroup extends DefaultActionGroup implements DumbAware, AnActionWithSyncUpdate {
    public CIFilterActionGroup() {
        super(
            ContinuousIntegrationImplLocalize.actionFilterText(),
            ContinuousIntegrationImplLocalize.actionFilterDescription(),
            PlatformIconGroup.generalFilter()
        );
        setPopup(true);

        for (CIBuildStatus status : CIBuildStatus.values()) {
            add(new CIStatusFilterAction(status));
        }
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getData(CIToolWindowPanel.KEY) != null);
    }
}
