/*
 * Copyright 2013 must-be.org
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
package consulo.lang;

import com.intellij.lang.Language;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 19:51/24.06.13
 */
public interface LanguageVersionWithParsing<T extends Language> extends LanguageVersion<T> {
  @NotNull
  Lexer createLexer();

  @NotNull
  PsiParser createParser();

  @NotNull
  TokenSet getWhitespaceTokens();

  @NotNull
  TokenSet getCommentTokens();

  @NotNull
  TokenSet getStringLiteralElements();
}
