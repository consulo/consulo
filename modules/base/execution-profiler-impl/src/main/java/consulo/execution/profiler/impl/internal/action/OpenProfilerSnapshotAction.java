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
package consulo.execution.profiler.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.execution.profiler.impl.internal.snapshot.ProfilerSnapshotService;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ActionImpl(
    id = OpenProfilerSnapshotAction.ID,
    parents = @ActionParentRef(
        value = @ActionRef(id = IdeActions.GROUP_RUN),
        anchor = ActionRefAnchor.BEFORE,
        relatedToAction = @ActionRef(id = IdeActions.ACTION_EDIT_RUN_CONFIGURATIONS)
    )
)
public class OpenProfilerSnapshotAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public static final String ID = "Profiler.OpenSnapshot";

    public OpenProfilerSnapshotAction() {
        super(
            LocalizeValue.localizeTODO("Open Profiler Snapshot..."),
            LocalizeValue.localizeTODO("Open a profiler snapshot in an editor tab"),
            PlatformIconGroup.actionsProfilecpu()
        );
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project != null) {
            project.getInstance(ProfilerSnapshotService.class).chooseAndOpen();
        }
    }
}
