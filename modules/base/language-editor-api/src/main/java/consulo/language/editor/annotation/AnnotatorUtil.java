/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.editor.annotation;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 02/01/2023
 */
public final class AnnotatorUtil {
  private AnnotatorUtil() {
  }

  @Nonnull
  @RequiredReadAction
  public static List<Annotation> runAnnotators(@Nonnull PsiFile psiFile, @Nonnull PsiElement contextElement) {
    List<AnnotatorFactory> annotatorFactories = AnnotatorFactory.forLanguage(psiFile.getProject(), psiFile.getLanguage());

    LanguageEditorInternalHelper helper = LanguageEditorInternalHelper.getInstance();
    List<Annotation> result = new ArrayList<>();
    for (AnnotatorFactory factory : annotatorFactories) {
      Annotator annotator = factory.createAnnotator();
      if (annotator == null) {
        continue;
      }

      result.addAll(helper.runAnnotator(annotator, psiFile, contextElement, false));
    }

    return result;
  }
}
