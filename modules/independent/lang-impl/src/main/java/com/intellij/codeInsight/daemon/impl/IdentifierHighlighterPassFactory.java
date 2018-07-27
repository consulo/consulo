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
import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class IdentifierHighlighterPassFactory implements TextEditorHighlightingPassFactory {
  public static boolean ourTestingIdentifierHighlighting = false;

  @Override
  public void register(@Nonnull Project project, @Nonnull TextEditorHighlightingPassRegistrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.UPDATE_ALL}, false, -1);
  }

  @Override
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull final PsiFile file, @Nonnull final Editor editor) {
    if (editor.isOneLineMode()) return null;

    if (CodeInsightSettings.getInstance().HIGHLIGHT_IDENTIFIER_UNDER_CARET && (!ApplicationManager.getApplication().isHeadlessEnvironment() || ourTestingIdentifierHighlighting)) {
      return new IdentifierHighlighterPass(file.getProject(), file, editor);
    }
    return null;
  }
}
