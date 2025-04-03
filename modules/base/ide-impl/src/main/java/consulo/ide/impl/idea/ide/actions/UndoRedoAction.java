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
package consulo.ide.impl.idea.ide.actions;

import consulo.language.editor.PlatformDataKeys;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.UndoManager;
import consulo.undoRedo.ApplicationUndoManager;
import consulo.undoRedo.ProjectUndoManager;
import consulo.dataContext.DataContext;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.util.lang.Couple;

public abstract class UndoRedoAction extends DumbAwareAction {
    public UndoRedoAction() {
        setEnabledInModalContext(true);
    }

    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        FileEditor editor = e.getData(FileEditor.KEY);
        UndoManager undoManager = getUndoManager(editor, dataContext);
        perform(editor, undoManager);
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        DataContext dataContext = event.getDataContext();
        FileEditor editor = event.getData(FileEditor.KEY);

        // do not allow global undo in dialogs
        if (editor == null) {
            Boolean isModalContext = event.getData(PlatformDataKeys.IS_MODAL_CONTEXT);
            if (isModalContext != null && isModalContext) {
                presentation.setEnabled(false);
                return;
            }
        }

        UndoManager undoManager = getUndoManager(editor, dataContext);
        if (undoManager == null) {
            presentation.setEnabled(false);
            return;
        }
        presentation.setEnabled(isAvailable(editor, undoManager));

        Couple<String> pair = getActionNameAndDescription(editor, undoManager);

        presentation.setText(pair.first);
        presentation.setDescription(pair.second);
    }

    private static UndoManager getUndoManager(FileEditor editor, DataContext dataContext) {
        Project project = getProject(editor, dataContext);
        return project != null ? ProjectUndoManager.getInstance(project) : ApplicationUndoManager.getInstance();
    }

    private static Project getProject(FileEditor editor, DataContext dataContext) {
        return editor instanceof TextEditor textEditor ? textEditor.getEditor().getProject() : dataContext.getData(Project.KEY);
    }

    protected abstract void perform(FileEditor editor, UndoManager undoManager);

    protected abstract boolean isAvailable(FileEditor editor, UndoManager undoManager);

    protected abstract Couple<String> getActionNameAndDescription(FileEditor editor, UndoManager undoManager);
}
