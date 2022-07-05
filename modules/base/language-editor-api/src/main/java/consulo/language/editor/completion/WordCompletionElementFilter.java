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

/*
 * @author max
 */
package consulo.language.editor.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiElement;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;

import javax.annotation.Nonnull;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface WordCompletionElementFilter extends LanguageExtension {
  ExtensionPointCacheKey<WordCompletionElementFilter, ByLanguageValue<WordCompletionElementFilter>> KEY =
          ExtensionPointCacheKey.create("WordCompletionElementFilter", LanguageOneToOne.build(new DefaultWordCompletionFilter()));

  @Nonnull
  static WordCompletionElementFilter forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(WordCompletionElementFilter.class).getOrBuildCache(KEY).requiredGet(language);
  }

  @RequiredReadAction
  static boolean isEnabledIn(@Nonnull ASTNode astNode) {
    final PsiElement psi = astNode.getPsi();
    if (psi == null) {
      return false;
    }
    IElementType elementType = astNode.getElementType();
    return forLanguage(psi.getLanguage()).isWordCompletionEnabledIn(elementType, LanguageVersionUtil.findLanguageVersion(elementType.getLanguage(), psi));
  }

  default boolean isWordCompletionEnabledIn(final IElementType element, LanguageVersion languageVersion) {
    final ParserDefinition parserDefinition = ParserDefinition.forLanguage(element.getLanguage());
    return parserDefinition != null && parserDefinition.getCommentTokens(languageVersion).contains(element);
  }
}