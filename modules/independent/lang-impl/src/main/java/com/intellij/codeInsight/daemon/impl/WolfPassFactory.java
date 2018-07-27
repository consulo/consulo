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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author cdr
 */
public class WolfPassFactory implements TextEditorHighlightingPassFactory {
  private static final Key<Long> ourModCount = Key.create("WolfPassFactory.ourModCount");

  @Override
  public void register(@Nonnull Project project, @Nonnull TextEditorHighlightingPassRegistrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, new int[]{Pass.UPDATE_ALL}, new int[]{Pass.LOCAL_INSPECTIONS}, false, Pass.WOLF);
  }

  @Override
  @Nullable
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull final Editor editor) {
    Long oldPsiModificationCount = file.getUserData(ourModCount);
    if (oldPsiModificationCount == null) {
      oldPsiModificationCount = 0L;
    }

    Project project = file.getProject();
    final long psiModificationCount = PsiManager.getInstance(project).getModificationTracker().getModificationCount();
    if (psiModificationCount == oldPsiModificationCount) {
      return null; //optimization
    }
    return new WolfHighlightingPass(project, editor.getDocument(), file) {
      @Override
      protected void applyInformationWithProgress() {
        super.applyInformationWithProgress();
        file.putUserData(ourModCount, psiModificationCount);
      }
    };
  }
}
