/*
 * Copyright 2013-2016 consulo.io
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
package consulo.language.version;

import consulo.language.parser.PsiParser;
import consulo.language.lexer.Lexer;
import consulo.language.ast.TokenSet;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19:51/24.06.13
 */
public interface LanguageVersionWithParsing {
  @Nonnull
  Lexer createLexer();

  @Nonnull
  PsiParser createParser();

  @Nonnull
  TokenSet getWhitespaceTokens();

  @Nonnull
  TokenSet getCommentTokens();

  @Nonnull
  TokenSet getStringLiteralElements();
}
