/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelveChangesManagerImpl;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "ShelvedChanges.ShowHideDeleted")
public class ShowHideRecycledAction extends AnAction {
    public ShowHideRecycledAction() {
        super(ActionLocalize.actionShelvedchangesShowhidedeletedText(), ActionLocalize.actionShelvedchangesShowhidedeletedDescription());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Presentation presentation = e.getPresentation();
        if (project == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        presentation.setEnabledAndVisible(true);
        boolean show = ShelveChangesManagerImpl.getInstance(project).isShowRecycled();
        presentation.setText(show ? "Hide Already Unshelved" : "Show Already Unshelved");
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ShelveChangesManagerImpl manager = ShelveChangesManagerImpl.getInstance(project);
        manager.setShowRecycled(!manager.isShowRecycled());
    }
}
