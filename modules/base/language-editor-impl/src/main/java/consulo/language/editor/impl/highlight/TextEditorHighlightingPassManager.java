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

package consulo.language.editor.impl.highlight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * User: anna
 * Date: 20-Apr-2006
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class TextEditorHighlightingPassManager {
  public static TextEditorHighlightingPassManager getInstance(Project project) {
    return project.getInstance(TextEditorHighlightingPassManager.class);
  }

  @Nonnull
  public abstract List<TextEditorHighlightingPass> instantiatePasses(@Nonnull PsiFile psiFile, @Nonnull Editor editor, @Nonnull int[] passesToIgnore);

  /**
   * Same as {@link #instantiateMainPasses(PsiFile, Document, HighlightInfoProcessor)} but with default process
   */
  @Nonnull
  public abstract List<TextEditorHighlightingPass> instantiateMainPasses(@Nonnull PsiFile psiFile, @Nonnull Document document);

  @Nonnull
  public abstract List<TextEditorHighlightingPass> instantiateMainPasses(@Nonnull PsiFile psiFile, @Nonnull Document document, @Nonnull HighlightInfoProcessor highlightInfoProcessor);

  @Nonnull
  public abstract List<DirtyScopeTrackingHighlightingPassFactory> getDirtyScopeTrackingFactories();
}
