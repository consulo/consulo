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

package consulo.execution.impl.internal.action;

import consulo.execution.executor.Executor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class BaseChooseRunConfigurationPopupAction extends AnAction {
    public BaseChooseRunConfigurationPopupAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    public BaseChooseRunConfigurationPopupAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected BaseChooseRunConfigurationPopupAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    protected BaseChooseRunConfigurationPopupAction(@Nonnull LocalizeValue text) {
        super(text);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        assert project != null;

        new ChooseRunConfigurationPopup(project, getAdKey(), getDefaultExecutor(), getAlternativeExecutor()).show();
    }

    protected abstract Executor getDefaultExecutor();

    protected abstract Executor getAlternativeExecutor();

    protected abstract String getAdKey();

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        final Project project = e.getData(Project.KEY);

        presentation.setEnabled(true);
        if (project == null || project.isDisposed()) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        }

        if (null == getDefaultExecutor()) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        }

        presentation.setEnabled(true);
        presentation.setVisible(true);
    }
}
