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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CutProvider;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "$Cut")
public class CutAction extends AnAction implements DumbAware {
    public CutAction() {
        super(
            ActionLocalize.action$cutText(),
            ActionLocalize.action$cutDescription(),
            PlatformIconGroup.actionsMenu_cut()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        CutProvider provider = e.getRequiredData(CutProvider.KEY);
        provider.performCut(e.getDataContext());
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        DataContext dataContext = event.getDataContext();
        CutProvider provider = event.getData(CutProvider.KEY);
        Project project = event.getData(Project.KEY);
        presentation.setEnabled(project != null && project.isOpen() && provider != null && provider.isCutEnabled(dataContext));
        if (event.getPlace().equals(ActionPlaces.EDITOR_POPUP) && provider != null) {
            presentation.setVisible(provider.isCutVisible(dataContext));
        }
        else {
            presentation.setVisible(true);
        }
    }
}
