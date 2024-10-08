/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.completion;

import consulo.language.parser.ParserDefinition;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.version.LanguageVersion;
import consulo.util.lang.ThreeState;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public abstract class SkipAutopopupInStrings extends CompletionConfidence {
  @Nonnull
  @Override
  public ThreeState shouldSkipAutopopup(@Nonnull PsiElement contextElement, @Nonnull PsiFile psiFile, int offset) {
    if (isInStringLiteral(contextElement)) {
      return ThreeState.YES;
    }

    return ThreeState.UNSURE;
  }

  public static boolean isInStringLiteral(PsiElement element) {
    LanguageVersion languageVersion = PsiUtilCore.findLanguageVersionFromElement(element);

    ParserDefinition definition = ParserDefinition.forLanguage(element.getApplication(), languageVersion.getLanguage());
    if (definition == null) {
      return false;
    }

    return isStringLiteral(element, definition, languageVersion) || isStringLiteral(element.getParent(), definition, languageVersion) ||
            isStringLiteralWithError(element, definition, languageVersion) || isStringLiteralWithError(element.getParent(), definition, languageVersion);
  }

  private static boolean isStringLiteral(PsiElement element, ParserDefinition definition, LanguageVersion languageVersion) {
    return PlatformPatterns.psiElement().withElementType(definition.getStringLiteralElements(languageVersion)).accepts(element);
  }

  private static boolean isStringLiteralWithError(PsiElement element, ParserDefinition definition, LanguageVersion languageVersion) {
    return isStringLiteral(element, definition, languageVersion) && PsiTreeUtil.nextLeaf(element) instanceof PsiErrorElement;
  }
}
