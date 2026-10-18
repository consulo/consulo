/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.dialog.DialogService;

@ActionImpl(id = "ShelvedChanges.CleanMarkedToDelete")
public class CleanUnshelvedAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public CleanUnshelvedAction() {
        super(
            LocalizeValue.localizeTODO("Clean Already Unshelved…"),
            LocalizeValue.localizeTODO("Delete unshelved changelists"),
            PlatformIconGroup.actionsGc()
        );
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Presentation presentation = e.getPresentation();
        if (project == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setVisible(true);
        presentation.setEnabled(!ShelveChangesManagerImpl.getInstance(project).getRecycledShelvedChangeLists().isEmpty());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        CleanUnshelvedDialogDescriptor descriptor = new CleanUnshelvedDialogDescriptor();
        project.getApplication().getInstance(DialogService.class).build(project, descriptor).showAsync().whenCompleteAsync((value, error) -> {
            if (error != null || value == null) {
                return;
            }
            ShelveChangesManagerImpl manager = ShelveChangesManagerImpl.getInstance(project);
            switch (descriptor.getScope()) {
                case OLDER_THAN -> manager.cleanUnshelved(false, descriptor.getTimePeriod().getTimeLimitInMillis());
                case ALL -> manager.clearRecycled();
                case CREATED_AUTOMATICALLY -> manager.cleanUnshelved(true, System.currentTimeMillis());
            }
        }, UIAccess.current());
    }
}
