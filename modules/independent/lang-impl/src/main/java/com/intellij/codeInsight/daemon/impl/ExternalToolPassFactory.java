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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.lang.ExternalLanguageAnnotators;
import com.intellij.lang.Language;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author cdr
 */
public class ExternalToolPassFactory implements TextEditorHighlightingPassFactory {
  private static final Key<MergingUpdateQueue> MERGING_UPDATE_QUEUE_KEY = Key.create("ExternalToolPassFactory.MergingUpdateQueue");

  @Override
  public void register(@Nonnull Project project, @Nonnull TextEditorHighlightingPassRegistrar registrar) {
    // start after PostHighlightingPass completion since it could report errors that can prevent us to run
    registrar.registerTextEditorHighlightingPass(this, new int[]{Pass.UPDATE_ALL}, null, true, Pass.EXTERNAL_TOOLS);

    MergingUpdateQueue queue = new MergingUpdateQueue("ExternalActivitiesQueue", 300, true, MergingUpdateQueue.ANY_COMPONENT, project, null, false);
    queue.setPassThrough(ApplicationManager.getApplication().isUnitTestMode());

    project.putUserData(MERGING_UPDATE_QUEUE_KEY, queue);
  }

  @Override
  @Nullable
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull final Editor editor) {
    TextRange textRange = FileStatusMap.getDirtyTextRange(editor, Pass.EXTERNAL_TOOLS) == null ? null : file.getTextRange();
    if (textRange == null || !externalAnnotatorsDefined(file)) {
      return null;
    }
    return new ExternalToolPass(this, file, editor, textRange.getStartOffset(), textRange.getEndOffset());
  }

  private static boolean externalAnnotatorsDefined(@Nonnull PsiFile file) {
    for (Language language : file.getViewProvider().getLanguages()) {
      final List<ExternalAnnotator> externalAnnotators = ExternalLanguageAnnotators.allForFile(language, file);
      if (!externalAnnotators.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  void scheduleExternalActivity(@Nonnull PsiFile file, @Nonnull Update update) {
    MergingUpdateQueue queue = file.getUserData(MERGING_UPDATE_QUEUE_KEY);
    assert queue != null;
    queue.queue(update);
  }
}
