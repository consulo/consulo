/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.ide.impl.psi.impl.cache;

import consulo.language.Language;
import consulo.language.parser.LanguageParserDefinitions;
import consulo.language.parser.ParserDefinition;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.version.LanguageVersion;

public class CacheUtil {

  public static boolean isInComments(final IElementType tokenType, LanguageVersion languageVersion) {
    final Language language = tokenType.getLanguage();

    //for (CommentTokenSetProvider provider : CommentTokenSetProvider.EXTENSION.allForLanguage(language)) {
    //  if (provider.isInComments(tokenType)) {
    //    return true;
    //  }
    //}

    boolean inComments = false;
    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language);

    if (parserDefinition != null) {
      final TokenSet commentTokens = parserDefinition.getCommentTokens(languageVersion);

      if (commentTokens.contains(tokenType)) {
        inComments = true;
      }
    }
    return inComments;
  }
}
