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

import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 19:52/24.06.13
 */
public abstract class LanguageVersionableParserDefinition implements ParserDefinition {
  @NotNull
  @Override
  public Lexer createLexer(@NotNull LanguageVersion languageVersion) {
    if(languageVersion instanceof LanguageVersionWithParsing) {
      return ((LanguageVersionWithParsing)languageVersion).createLexer();
    }
    throw new IllegalArgumentException("'createLexer' need override for language version '" + languageVersion + "'");
  }

  @NotNull
  @Override
  public PsiParser createParser(@NotNull LanguageVersion languageVersion) {
    if(languageVersion instanceof LanguageVersionWithParsing) {
      return ((LanguageVersionWithParsing)languageVersion).createParser();
    }
    throw new IllegalArgumentException("'createParser' need override for language version '" + languageVersion + "'");
  }

  @NotNull
  @Override
  public TokenSet getWhitespaceTokens(@NotNull LanguageVersion languageVersion) {
    if(languageVersion instanceof LanguageVersionWithParsing) {
      return ((LanguageVersionWithParsing)languageVersion).getWhitespaceTokens();
    }
    throw new IllegalArgumentException("'getWhitespaceTokens' need override for language version '" + languageVersion + "'");
  }

  @NotNull
  @Override
  public TokenSet getCommentTokens(@NotNull LanguageVersion languageVersion) {
    if(languageVersion instanceof LanguageVersionWithParsing) {
      return ((LanguageVersionWithParsing)languageVersion).getCommentTokens();
    }
    throw new IllegalArgumentException("'getCommentTokens' need override for language version '" + languageVersion + "'");
  }

  @NotNull
  @Override
  public TokenSet getStringLiteralElements(@NotNull LanguageVersion languageVersion) {
    if(languageVersion instanceof LanguageVersionWithParsing) {
      return ((LanguageVersionWithParsing)languageVersion).getStringLiteralElements();
    }
    throw new IllegalArgumentException("'getStringLiteralElements' need override for language version '" + languageVersion + "'");
  }
}
