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
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ActionImpl(id = "Dvcs.Log.Branches.GroupByDirectory")
public class DvcsBranchesGroupByDirectoryAction extends ToggleAction implements DumbAware {
    public DvcsBranchesGroupByDirectoryAction() {
        super(DistributedVcsLocalize.branchesPaneActionGroupByDirectoryText(), LocalizeValue.empty(), PlatformIconGroup.actionsGroupbypackage());
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabledAndVisible(e.hasData(DvcsBranchesLogSidePanel.KEY));
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        DvcsBranchesLogSidePanel panel = e.getData(DvcsBranchesLogSidePanel.KEY);
        return panel != null && panel.isDirectoryGrouping();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean state) {
        DvcsBranchesLogSidePanel panel = e.getData(DvcsBranchesLogSidePanel.KEY);
        if (panel != null) {
            panel.setDirectoryGrouping(state);
        }
    }
}
