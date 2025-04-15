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
package consulo.compiler.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.compiler.CompilerManager;
import consulo.compiler.CompilerRunner;
import consulo.compiler.action.CompileActionBase;
import consulo.component.extension.ExtensionPoint;
import consulo.dataContext.DataContext;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "CompileDirty")
public class CompileDirtyAction extends CompileActionBase {
    public CompileDirtyAction() {
        super(ActionLocalize.actionCompiledirtyText(), ActionLocalize.actionCompiledirtyDescription(), PlatformIconGroup.actionsCompile());
    }

    @Override
    @RequiredUIAccess
    protected void doAction(DataContext dataContext, Project project) {
        CompilerManager.getInstance(project).make(null);
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent event) {
        super.update(event);
        Presentation presentation = event.getPresentation();
        if (!presentation.isEnabled()) {
            return;
        }

        Project project = event.getData(Project.KEY);
        presentation.setEnabled(project != null);

        if (project != null) {
            ExtensionPoint<CompilerRunner> point = project.getExtensionPoint(CompilerRunner.class);
            CompilerRunner runner = point.findFirstSafe(CompilerRunner::isAvailable);
            if (runner != null) {
                presentation.setIcon(runner.getBuildIcon());
            } else {
                presentation.setIcon(PlatformIconGroup.actionsCompile());
            }
        } else {
            presentation.setIcon(PlatformIconGroup.actionsCompile());
        }
    }
}