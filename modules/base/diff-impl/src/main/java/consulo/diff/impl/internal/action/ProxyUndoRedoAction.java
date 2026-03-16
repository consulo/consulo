/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.action;

import consulo.codeEditor.Editor;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.EmptyAction;
import consulo.ui.ex.action.IdeActions;
import consulo.undoRedo.ApplicationUndoManager;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.UndoManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

public class ProxyUndoRedoAction extends DumbAwareAction {
  
  private final UndoManager myUndoManager;
  
  private final TextEditor myEditor;
  private final boolean myUndo;

  private ProxyUndoRedoAction(UndoManager manager, TextEditor editor, boolean undo) {
    myUndoManager = manager;
    myEditor = editor;
    myUndo = undo;
  }

  public static void register(@Nullable Project project, Editor editor, JComponent component) {
    UndoManager undoManager = project != null ? ProjectUndoManager.getInstance(project) : ApplicationUndoManager.getInstance();
    TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(editor);
    if (undoManager != null) {
      EmptyAction.setupAction(new ProxyUndoRedoAction(undoManager, textEditor, true), IdeActions.ACTION_UNDO, component);
      EmptyAction.setupAction(new ProxyUndoRedoAction(undoManager, textEditor, false), IdeActions.ACTION_REDO, component);
    }
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(myUndo ? myUndoManager.isUndoAvailable(myEditor) : myUndoManager.isRedoAvailable(myEditor));
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    if (myUndo) {
      myUndoManager.undo(myEditor);
    }
    else {
      myUndoManager.redo(myEditor);
    }
  }
}
