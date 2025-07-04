/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.tasks.actions;

import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.tasks.doc.TaskPsiElement;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.task.LocalTask;
import consulo.task.impl.internal.action.BaseTaskAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

/**
 * @author Dennis.Ushakov
 */
public class ShowTaskDescription extends BaseTaskAction {
    @Override
    public void update(@Nonnull AnActionEvent event) {
        super.update(event);
        if (event.getPresentation().isEnabled()) {
            final Presentation presentation = event.getPresentation();
            final LocalTask activeTask = getActiveTask(event);
            presentation.setEnabled(activeTask != null && activeTask.isIssue() && activeTask.getDescription() != null);
            if (activeTask == null || !activeTask.isIssue()) {
                presentation.setTextValue(getTemplatePresentation().getTextValue());
            }
            else {
                presentation.setText("Show '" + activeTask.getPresentableName() + "' _Description");
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final Project project = getProject(e);
        assert project != null;
        final LocalTask task = getActiveTask(e);
        FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.quickjavadoc.ctrln");
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(getTemplatePresentation().getTextValue())
            .run(
                () -> DocumentationManager.getInstance(project)
                    .showJavaDocInfo(new TaskPsiElement(PsiManager.getInstance(project), task), null)
            );
    }
}
