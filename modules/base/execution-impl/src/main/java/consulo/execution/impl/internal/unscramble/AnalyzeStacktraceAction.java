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

package consulo.execution.impl.internal.unscramble;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.unscramble.UnscrambleService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.configurable.ConfigurableAction;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ActionImpl(
    id = "AnalyzeStacktrace",
    parents = @ActionParentRef(
        value = @ActionRef(id = IdeActions.ACTION_CODE_MENU),
        relatedToAction = @ActionRef(id = "AnalyzeMenu"),
        anchor = ActionRefAnchor.AFTER
    )
)
public class AnalyzeStacktraceAction extends ConfigurableAction implements DumbAware {
    private ValidData<Project> myProject;

    @Override
    protected void init(@Nonnull Builder builder) {
        builder.text(ExecutionLocalize.actionAnalyzestacktraceText());
        myProject = builder.disabledIfAbsent(Project.KEY);
    }

    @Override
    @RequiredUIAccess
    protected void performAction(@Nonnull AnActionEvent e) {
        myProject.get().getInstance(UnscrambleService.class).showAsync();
    }
}
