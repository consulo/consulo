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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoableAction;
import consulo.undoRedo.UnexpectedUndoException;
import consulo.project.Project;

/**
 * @author Dmitry Avdeev
 */
public class UndoableCommand {
    @RequiredUIAccess
    public static void execute(final Project project, final UndoableAction action, String name, String groupId) {
        CommandProcessor.getInstance().newCommand(() -> {
                try {
                    action.redo();
                }
                catch (UnexpectedUndoException e) {
                    throw new RuntimeException(e);
                }
                ProjectUndoManager.getInstance(project).undoableActionPerformed(action);
            })
            .withProject(project)
            .withName(LocalizeValue.ofNullable(name))
            .withGroupId(groupId)
            .execute();
    }
}
