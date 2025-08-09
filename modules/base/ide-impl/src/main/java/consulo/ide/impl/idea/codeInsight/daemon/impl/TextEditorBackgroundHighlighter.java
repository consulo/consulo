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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.fileEditor.highlight.BackgroundEditorHighlighter;
import consulo.language.editor.Pass;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPassManager;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiCompiledFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileEx;
import consulo.logging.Logger;

import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class TextEditorBackgroundHighlighter implements BackgroundEditorHighlighter {
  private static final Logger LOG = Logger.getInstance(TextEditorBackgroundHighlighter.class);
  private static final int[] EXCEPT_OVERRIDDEN = {
          Pass.UPDATE_FOLDING,
          Pass.POPUP_HINTS,
          Pass.UPDATE_ALL,
          Pass.LOCAL_INSPECTIONS,
          Pass.WHOLE_FILE_LOCAL_INSPECTIONS,
          Pass.EXTERNAL_TOOLS,
  };

  private final Editor myEditor;
  private final Document myDocument;
  private PsiFile myFile;
  private final Project myProject;
  private boolean myCompiled;

  public TextEditorBackgroundHighlighter(@Nonnull Project project, @Nonnull Editor editor) {
    myProject = project;
    myEditor = editor;
    myDocument = myEditor.getDocument();
    renewFile();
  }

  private void renewFile() {
    if (myFile == null || !myFile.isValid()) {
      myFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
      myCompiled = myFile instanceof PsiCompiledFile;
      if (myCompiled) {
        myFile = ((PsiCompiledFile)myFile).getDecompiledPsiFile();
      }
      if (myFile != null && !myFile.isValid()) {
        myFile = null;
      }
    }

    if (myFile != null) {
      myFile.putUserData(PsiFileEx.BATCH_REFERENCE_PROCESSING, Boolean.TRUE);
    }
  }

  @Nonnull
  List<TextEditorHighlightingPass> getPasses(@Nonnull int[] passesToIgnore) {
    if (myProject.isDisposed()) return Collections.emptyList();
    LOG.assertTrue(PsiDocumentManager.getInstance(myProject).isCommitted(myDocument));
    renewFile();
    if (myFile == null) return Collections.emptyList();
    if (myCompiled) {
      passesToIgnore = EXCEPT_OVERRIDDEN;
    }
    else if (!DaemonCodeAnalyzer.getInstance(myProject).isHighlightingAvailable(myFile)) {
      return Collections.emptyList();
    }

    TextEditorHighlightingPassManager passRegistrar = TextEditorHighlightingPassManager.getInstance(myProject);

    return passRegistrar.instantiatePasses(myFile, myEditor, passesToIgnore);
  }

  @Override
  @Nonnull
  public TextEditorHighlightingPass[] createPassesForVisibleArea() {
    return createPassesForEditor();
  }

  @Override
  @Nonnull
  public TextEditorHighlightingPass[] createPassesForEditor() {
    List<TextEditorHighlightingPass> passes = getPasses(ArrayUtil.EMPTY_INT_ARRAY);
    return passes.isEmpty() ? TextEditorHighlightingPass.EMPTY_ARRAY : passes.toArray(new TextEditorHighlightingPass[passes.size()]);
  }
}
