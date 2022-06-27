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

import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.parser.ParserDefinition;
import consulo.language.version.LanguageVersion;

import javax.annotation.Nonnull;

class DefaultWordCompletionFilter implements WordCompletionElementFilter {
  @Override
  public boolean isWordCompletionEnabledIn(final IElementType element, LanguageVersion languageVersion) {
    final ParserDefinition parserDefinition = ParserDefinition.forLanguage(element.getLanguage());
    return parserDefinition != null && parserDefinition.getCommentTokens(languageVersion).contains(element);
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}