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
package consulo.ide.impl.idea.openapi.vcs.checkout;

import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import jakarta.annotation.Nonnull;

public class CheckoutAction extends DumbAwareAction {
    private final CheckoutProvider myProvider;

    public CheckoutAction(CheckoutProvider provider) {
        super(provider.getName());
        myProvider = provider;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        project = (project == null) ? ProjectManager.getInstance().getDefaultProject() : project;
        myProvider.doCheckout(project, ProjectLevelVcsManager.getInstance(project).getCompositeCheckoutListener());
    }
}
