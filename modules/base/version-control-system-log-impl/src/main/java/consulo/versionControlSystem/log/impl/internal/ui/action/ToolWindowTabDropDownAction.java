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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.log.internal.VcsLogActionPlaces;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ActionImpl(id = "Vcs.Log.ToolWindow.TabActions.DropDownButton")
public class ToolWindowTabDropDownAction extends VcsLogGearActionGroup {
    public ToolWindowTabDropDownAction() {
        super(
            VersionControlSystemLogLocalize.groupToolWindowTabActionsText(),
            VersionControlSystemLogLocalize.groupToolWindowTabActionsText(),
            PlatformIconGroup.generalArrowdown(),
            VcsLogActionPlaces.TOOL_WINDOW_TAB_DROP_DOWN_ACTION_GROUP
        );
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY));
    }
}
