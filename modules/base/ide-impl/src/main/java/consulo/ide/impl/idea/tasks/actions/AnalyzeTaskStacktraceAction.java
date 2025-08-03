/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.ide.impl.idea.tasks.actions;

import consulo.annotation.component.ActionImpl;
import consulo.execution.unscramble.AnalyzeStacktraceUtil;
import consulo.project.Project;
import consulo.task.Comment;
import consulo.task.LocalTask;
import consulo.task.Task;
import consulo.task.impl.internal.action.BaseTaskAction;
import consulo.task.localize.TaskLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 */
@ActionImpl(id = "tasks.analyze.stacktrace")
public class AnalyzeTaskStacktraceAction extends BaseTaskAction {
    public AnalyzeTaskStacktraceAction() {
        super(TaskLocalize.actionTasksAnalyzeStacktraceText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        LocalTask activeTask = getActiveTask(e);
        Project project = e.getRequiredData(Project.KEY);
        assert activeTask != null;
        analyzeStacktrace(activeTask, project);
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        super.update(event);
        if (event.getPresentation().isEnabled()) {
            Task activeTask = getActiveTask(event);
            event.getPresentation().setEnabled(activeTask != null && hasTexts(activeTask));
        }
    }

    public static boolean hasTexts(Task activeTask) {
        return (activeTask.getDescription() != null || activeTask.getComments().length > 0);
    }

    @RequiredUIAccess
    public static void analyzeStacktrace(Task task, Project project) {
        ChooseStacktraceDialog stacktraceDialog = new ChooseStacktraceDialog(project, task);
        stacktraceDialog.show();
        if (stacktraceDialog.isOK() && stacktraceDialog.getTraces().length > 0) {
            Comment[] comments = stacktraceDialog.getTraces();
            for (Comment comment : comments) {
                AnalyzeStacktraceUtil.addConsole(project, null, task.getId(), comment.getText());
            }
        }
    }
}
