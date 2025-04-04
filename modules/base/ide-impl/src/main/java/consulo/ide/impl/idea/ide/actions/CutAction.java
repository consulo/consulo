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

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CutProvider;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public class CutAction extends AnAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        CutProvider provider = e.getData(CutProvider.KEY);
        if (provider == null) {
            return;
        }
        provider.performCut(dataContext);
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent event) {
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
