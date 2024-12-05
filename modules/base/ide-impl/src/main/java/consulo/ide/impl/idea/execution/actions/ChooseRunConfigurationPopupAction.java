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

package consulo.ide.impl.idea.execution.actions;

import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

public class ChooseRunConfigurationPopupAction extends AnAction {
    @Inject
    public ChooseRunConfigurationPopupAction() {
        super("Run...", "Choose and run configuration", PlatformIconGroup.actionsExecute());
    }

    public ChooseRunConfigurationPopupAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    public ChooseRunConfigurationPopupAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        assert project != null;

        new ChooseRunConfigurationPopup(project, getAdKey(), getDefaultExecutor(), getAlternativeExecutor()).show();
    }

    protected Executor getDefaultExecutor() {
        return DefaultRunExecutor.getRunExecutorInstance();
    }

    protected Executor getAlternativeExecutor() {
        return ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);
    }

    protected String getAdKey() {
        return "run.configuration.alternate.action.ad";
    }

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
