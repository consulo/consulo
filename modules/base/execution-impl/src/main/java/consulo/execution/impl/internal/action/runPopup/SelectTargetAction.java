/*
 * Copyright 2013-2025 consulo.io
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
package consulo.execution.impl.internal.action.runPopup;

import consulo.execution.ExecutionTarget;
import consulo.execution.ExecutionTargetManager;
import consulo.execution.impl.internal.action.RunConfigurationsComboBoxAction;
import consulo.execution.internal.RunManagerEx;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import jakarta.annotation.Nonnull;

public class SelectTargetAction extends AnAction {
    private final Project myProject;
    private final ExecutionTarget myTarget;

    public SelectTargetAction(Project project, ExecutionTarget target, boolean selected) {
        myProject = project;
        myTarget = target;

        String name = target.getDisplayName();
        Presentation presentation = getTemplatePresentation();
        presentation.setDisabledMnemonic(true);
        presentation.setTextValue(LocalizeValue.of(name));
        presentation.setDescriptionValue(LocalizeValue.localizeTODO("Select " + name));

        presentation.setIcon(
            selected
                ? ImageEffects.resize(PlatformIconGroup.actionsChecked(), Image.DEFAULT_ICON_SIZE)
                : Image.empty(Image.DEFAULT_ICON_SIZE)
        );
        presentation.setSelectedIcon(
            selected
                ? ImageEffects.resize(PlatformIconGroup.actionsChecked_selected(), Image.DEFAULT_ICON_SIZE)
                : Image.empty(Image.DEFAULT_ICON_SIZE)
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ExecutionTargetManager.setActiveTarget(myProject, myTarget);
        RunConfigurationsComboBoxAction.updatePresentation(ExecutionTargetManager.getActiveTarget(myProject), RunManagerEx.getInstanceEx(myProject).getSelectedConfiguration(), myProject, e.getPresentation(), e.getPlace());
    }
}
