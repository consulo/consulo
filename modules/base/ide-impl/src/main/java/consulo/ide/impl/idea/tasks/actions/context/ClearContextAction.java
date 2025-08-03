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
package consulo.ide.impl.idea.tasks.actions.context;

import consulo.annotation.component.ActionImpl;
import consulo.task.localize.TaskLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.undoRedo.GlobalUndoableAction;
import consulo.undoRedo.UnexpectedUndoException;
import consulo.project.Project;
import consulo.task.impl.internal.action.BaseTaskAction;
import consulo.task.impl.internal.context.WorkingContextManager;
import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 */
@ActionImpl(id = "context.clear")
public class ClearContextAction extends BaseTaskAction {
    public ClearContextAction() {
        super(TaskLocalize.actionContextClearText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        GlobalUndoableAction action = new GlobalUndoableAction() {
            @Override
            public void undo() throws UnexpectedUndoException {
            }

            @Override
            public void redo() throws UnexpectedUndoException {
                WorkingContextManager.getInstance(project).clearContext();
            }
        };
        UndoableCommand.execute(project, action, TaskLocalize.taskClearContextActionName().get(), "Context");
    }
}
