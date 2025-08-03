/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.internal.psiView;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "PsiViewer")
public class PsiViewerAction extends AnAction implements DumbAware {
    @Nonnull
    private final Application myApplication;

    @Inject
    public PsiViewerAction(@Nonnull Application application) {
        super(ActionLocalize.actionPsiviewerText());
        myApplication = application;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        new PsiViewerDialog(project, false, null, null).show();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY) && myApplication.isInternal());
    }
}
