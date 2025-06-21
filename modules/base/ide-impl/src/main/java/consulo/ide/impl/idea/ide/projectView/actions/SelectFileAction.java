/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.idea.ide.projectView.actions;

import consulo.annotation.component.ActionImpl;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2021-02-27
 */
@ActionImpl(id = "SelectInProjectView")
public class SelectFileAction extends DumbAwareAction {
    public SelectFileAction() {
        super(
            ProjectUIViewLocalize.actionScrollFromSourceText(),
            ProjectUIViewLocalize.actionScrollFromSourceDescription(),
            PlatformIconGroup.generalLocate()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        ProjectViewImpl projectView = (ProjectViewImpl) ProjectView.getInstance(project);

        projectView.scrollFromSource();
    }
}
