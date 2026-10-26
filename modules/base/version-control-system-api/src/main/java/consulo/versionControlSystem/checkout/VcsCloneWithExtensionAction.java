// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.checkout;

import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public abstract class VcsCloneWithExtensionAction extends DumbAwareAction {
    protected VcsCloneWithExtensionAction() {
    }

    protected VcsCloneWithExtensionAction(LocalizeValue text, LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    public abstract Class<? extends CheckoutProvider> getExtension();

    @RequiredUIAccess
    @Override
    public final void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Project currentOrDefaultProject = project == null ? ProjectManager.getInstance().getDefaultProject() : project;
        Application application = Application.get();
        CheckoutProvider provider = application.getExtensionPoint(CheckoutProvider.class).findExtension(getExtension());
        if (provider == null) {
            return;
        }
        application.getInstance(CheckoutDialogService.class).showCheckoutDialog(currentOrDefaultProject, provider);
    }
}
