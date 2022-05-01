/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.util;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.virtualFileSystem.NonPhysicalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PsiEditorUtil {
  private static final Logger LOG = Logger.getInstance(PsiEditorUtil.class);

  /**
   * Tries to find editor for the given element.
   * <p/>
   * There are at least two approaches to achieve the target. Current method is intended to encapsulate both of them:
   * <ul>
   * <li>target editor works with a real file that remains at file system;</li>
   * <li>target editor works with a virtual file;</li>
   * </ul>
   * <p/>
   * Please don't use this method for finding an editor for quick fix.
   *
   * @param element target element
   * @return editor that works with a given element if the one is found; <code>null</code> otherwise
   * @see {@link consulo.ide.impl.idea.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement}
   */
  @Nullable
  @RequiredUIAccess
  public static Editor findEditor(@Nonnull PsiElement element) {
    PsiFile psiFile = element.getContainingFile();
    VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
    if (virtualFile == null) {
      return null;
    }

    Project project = psiFile.getProject();
    if (virtualFile.isInLocalFileSystem() || virtualFile.getFileSystem() instanceof NonPhysicalFileSystem) {
      // Try to find editor for the real file.
      final FileEditor[] editors = FileEditorManager.getInstance(project).getEditors(virtualFile);
      for (FileEditor editor : editors) {
        if (editor instanceof TextEditor) {
          return ((TextEditor)editor).getEditor();
        }
      }
    }

    // We assume that data context from focus-based retrieval should success if performed from EDT.
    AsyncResult<DataContext> asyncResult = DataManager.getInstance().getDataContextFromFocus();
    if (asyncResult.isDone()) {
      Editor editor = asyncResult.getResult().getData(Editor.KEY);
      if (editor != null) {
        Document cachedDocument = PsiDocumentManager.getInstance(project).getCachedDocument(psiFile);
        // Ensure that target editor is found by checking its document against the one from given PSI element.
        if (cachedDocument == editor.getDocument()) {
          return editor;
        }
      }
    }
    return null;
  }
}
