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
package consulo.versionControlSystem.impl.internal.checkout;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.DialogService;
import consulo.versionControlSystem.checkout.CheckoutProvider;

public class CheckoutAction extends DumbAwareAction {
    private final CheckoutProvider myProvider;
    private final DialogService myDialogService;

    public CheckoutAction(CheckoutProvider provider, DialogService dialogService) {
        super(provider.getName(), LocalizeValue.empty(), provider.getIcon());
        myProvider = provider;
        myDialogService = dialogService;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        project = (project == null) ? ProjectManager.getInstance().getDefaultProject() : project;

        myDialogService.build(project, new CheckoutDialogDescriptor(project, myProvider)).showAsync();
    }
}
