/*
 * Copyright 2013-2019 consulo.io
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
package consulo.editor.actionSystem;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-01-19
 */
public abstract class DocumentEditorActionHandler extends EditorActionHandler {
  protected DocumentEditorActionHandler() {
  }

  protected DocumentEditorActionHandler(boolean runForEachCaret) {
    super(runForEachCaret);
  }

  @RequiredUIAccess
  @Override
  protected final void doExecuteAsync(Editor editor, @Nullable Caret caret, @Nullable DataContext dataContext, @Nonnull AsyncResult<Void> asyncResult) {
    if (editor.isViewer()) {
      asyncResult.setDone();
      return;
    }

    AsyncResult<Void> afterWriteCheck = AsyncResult.undefined();

    if (dataContext != null) {
      Project project = dataContext.getData(CommonDataKeys.PROJECT);
      if (project != null) {
        FileDocumentManager.getInstance().requestWritingAsync(editor.getDocument(), UIAccess.current(), project).notify(afterWriteCheck);
      }
      else {
        afterWriteCheck.setDone();
      }
    }
    else {
      afterWriteCheck.setDone();
    }

    afterWriteCheck.doWhenDone(() -> {
      final Document doc = editor.getDocument();

      doc.startGuardedBlockChecking();
      asyncResult.doWhenDone(doc::stopGuardedBlockChecking);

      try {
        executeDocumentAction(editor, caret, dataContext, asyncResult);
      }
      catch (ReadOnlyFragmentModificationException e) {
        EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(doc).handle(e);
      }
    });
  }

  @RequiredUIAccess
  public abstract void executeDocumentAction(Editor editor, @Nullable Caret caret, @Nullable DataContext dataContext, @Nonnull AsyncResult<Void> asyncResult);
}
