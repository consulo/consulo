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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.ide.impl.idea.codeInsight.folding.impl.FoldingUpdate;
import consulo.codeEditor.Editor;
import consulo.application.progress.ProgressIndicator;
import consulo.application.dumb.IndexNotReadyException;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

class InjectedCodeFoldingPass extends TextEditorHighlightingPass {
  private static final Key<Boolean> THE_FIRST_TIME_KEY = Key.create("FirstInjectedFoldingPass");
  private Runnable myRunnable;
  private final Editor myEditor;
  private final PsiFile myFile;

  InjectedCodeFoldingPass(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    super(project, editor.getDocument(), false);
    myEditor = editor;
    myFile = file;
  }

  @Override
  public void doCollectInformation(@Nonnull ProgressIndicator progress) {
    boolean firstTime = CodeFoldingPass.isFirstTime(myFile, myEditor, THE_FIRST_TIME_KEY);
    Runnable runnable = FoldingUpdate.updateInjectedFoldRegions(myEditor, myFile, firstTime);
    synchronized (this) {
      myRunnable = runnable;
    }
  }

  @Override
  public void doApplyInformationToEditor() {
    Runnable runnable;
    synchronized (this) {
      runnable = myRunnable;
    }
    if (runnable != null) {
      try {
        runnable.run();
      }
      catch (IndexNotReadyException ignored) {
      }
      CodeFoldingPass.clearFirstTimeFlag(myFile, myEditor, THE_FIRST_TIME_KEY);
    }
  }
}