/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.navigationToolbar;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.UISettings;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "ShowNavBar")
public class ShowNavBarAction extends AnAction implements DumbAware, PopupAction {
    public ShowNavBarAction() {
        super(ActionLocalize.actionShownavbarText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        UISettings uiSettings = UISettings.getInstance();
        if (uiSettings.SHOW_NAVIGATION_BAR && !uiSettings.PRESENTATION_MODE) {
            new SelectInNavBarTarget(project).select(null, false);
        }
        else {
            project.getInstance(EmbeddedNavService.class).show(e.getDataContext());
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }
}
