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
package consulo.versionControlSystem.distributed.impl.internal.branch;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.distributed.branch.DvcsBranchDataKeys;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ActionImpl(id = "Dvcs.Log.Branches.ShowInLog")
public class DvcsBranchesShowInLogAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public DvcsBranchesShowInLogAction() {
        super(DistributedVcsLocalize.branchesPaneActionShowInLogText(), LocalizeValue.empty(), PlatformIconGroup.generalLocate());
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setVisible(e.hasData(DvcsBranchesLogSidePanel.KEY));
        e.getPresentation().setEnabled(e.hasData(DvcsBranchDataKeys.SELECTED_REF));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DvcsBranchesLogSidePanel panel = e.getData(DvcsBranchesLogSidePanel.KEY);
        if (panel != null) {
            panel.filterBySelectedRef();
        }
    }
}
