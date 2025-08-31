/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.codeEditor.action;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.DocumentRunnable;
import consulo.document.FileDocumentManager;
import consulo.document.ReadOnlyFragmentModificationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base class for {@link EditorActionHandler} instances, which need to modify the document.
 * Implementations should override {@link #executeWriteAction(Editor, Caret, DataContext)}.
 */
public abstract class EditorWriteActionHandler extends EditorActionHandler {
  private boolean inExecution;

  protected EditorWriteActionHandler() {
  }

  protected EditorWriteActionHandler(boolean runForEachCaret) {
    super(runForEachCaret);
  }

  @Override
  @RequiredUIAccess
  public void doExecute(final Editor editor, @Nullable final Caret caret, final DataContext dataContext) {
    if (editor.isViewer()) return;

    if (dataContext != null) {
      Project project = dataContext.getData(Project.KEY);
      if (project != null && !FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) return;
    }

    ApplicationManager.getApplication().runWriteAction(new DocumentRunnable(editor.getDocument(), editor.getProject()) {
      @Override
      public void run() {
        Document doc = editor.getDocument();

        doc.startGuardedBlockChecking();
        try {
          executeWriteAction(editor, caret, dataContext);
        }
        catch (ReadOnlyFragmentModificationException e) {
          EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(doc).handle(e);
        }
        finally {
          doc.stopGuardedBlockChecking();
        }
      }
    });
  }

  /**
   * @deprecated Use/override
   * {@link #executeWriteAction(Editor, Caret, DataContext)}
   * instead.
   */
  @RequiredWriteAction
  public void executeWriteAction(Editor editor, DataContext dataContext) {
    if (inExecution) {
      return;
    }
    try {
      inExecution = true;
      executeWriteAction(editor, editor.getCaretModel().getCurrentCaret(), dataContext);
    }
    finally {
      inExecution = false;
    }
  }

  @RequiredWriteAction
  public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
    if (inExecution) {
      return;
    }
    try {
      inExecution = true;
      //noinspection deprecation
      executeWriteAction(editor, dataContext);
    }
    finally {
      inExecution = false;
    }
  }

  public static abstract class ForEachCaret extends EditorWriteActionHandler {
    protected ForEachCaret() {
      super(true);
    }

    @Override
    public abstract void executeWriteAction(@Nonnull Editor editor, @SuppressWarnings("NullableProblems") @Nonnull Caret caret, DataContext dataContext);
  }
}
