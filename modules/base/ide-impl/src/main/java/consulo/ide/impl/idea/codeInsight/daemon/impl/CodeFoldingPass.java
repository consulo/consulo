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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.language.editor.impl.highlight.EditorBoundHighlightingPass;
import consulo.language.editor.folding.CodeFoldingManager;
import consulo.ide.impl.idea.codeInsight.folding.impl.FoldingUpdate;
import consulo.language.inject.InjectedLanguageManager;
import consulo.codeEditor.Editor;
import consulo.application.progress.ProgressIndicator;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.dumb.PossiblyDumbAware;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import consulo.annotation.access.RequiredReadAction;

class CodeFoldingPass extends EditorBoundHighlightingPass implements PossiblyDumbAware {
  private static final Key<Boolean> THE_FIRST_TIME = Key.create("FirstFoldingPass");
  private volatile Runnable myRunnable;

  CodeFoldingPass(@Nonnull Editor editor, @Nonnull PsiFile file) {
    super(editor, file, false);
  }

  @RequiredReadAction
  @Override
  public void doCollectInformation(@Nonnull ProgressIndicator progress) {
    final boolean firstTime = isFirstTime(myFile, myEditor, THE_FIRST_TIME);
    myRunnable = CodeFoldingManager.getInstance(myProject).updateFoldRegionsAsync(myEditor, firstTime);
  }

  static boolean isFirstTime(PsiFile file, Editor editor, Key<Boolean> key) {
    return file.getUserData(key) == null || editor.getUserData(key) == null;
  }

  static void clearFirstTimeFlag(PsiFile file, Editor editor, Key<Boolean> key) {
    file.putUserData(key, Boolean.FALSE);
    editor.putUserData(key, Boolean.FALSE);
  }

  @Override
  public void doApplyInformationToEditor() {
    Runnable runnable = myRunnable;
    if (runnable != null){
      try {
        runnable.run();
      }
      catch (IndexNotReadyException ignored) {
      }
    }

    if (InjectedLanguageManager.getInstance(myFile.getProject()).getTopLevelFile(myFile) == myFile) {
      clearFirstTimeFlag(myFile, myEditor, THE_FIRST_TIME);
    }
  }

  /**
   * Checks the ability to update folding in the Dumb Mode. True by default.
   * @return true if the language implementation can update folding ranges
   */
  @Override
  public boolean isDumbAware() {
    return FoldingUpdate.supportsDumbModeFolding(myEditor);
  }
}
