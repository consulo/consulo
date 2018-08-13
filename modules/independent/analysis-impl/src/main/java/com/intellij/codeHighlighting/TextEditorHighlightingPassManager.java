/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.codeHighlighting;

import com.intellij.codeInsight.daemon.impl.HighlightInfoProcessor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * User: anna
 * Date: 20-Apr-2006
 */
public abstract class TextEditorHighlightingPassManager {
  public static TextEditorHighlightingPassManager getInstance(Project project) {
    return ServiceManager.getService(project, TextEditorHighlightingPassManager.class);
  }

  @Nonnull
  public abstract List<TextEditorHighlightingPass> instantiatePasses(@Nonnull PsiFile psiFile, @Nonnull Editor editor, @Nonnull int[] passesToIgnore);

  @Nonnull
  public abstract List<TextEditorHighlightingPass> instantiateMainPasses(@Nonnull PsiFile psiFile, @Nonnull Document document, @Nonnull HighlightInfoProcessor highlightInfoProcessor);

  @Nonnull
  public abstract List<DirtyScopeTrackingHighlightingPassFactory> getDirtyScopeTrackingFactories();
}
