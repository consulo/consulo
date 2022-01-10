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
package consulo.fileTypes;

import consulo.lang.LanguageVersion;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21:11/24.06.13
 */
public abstract class LanguageVersionableSyntaxHighlighter extends SyntaxHighlighterBase {
  private final LanguageVersion myLanguageVersion;

  public LanguageVersionableSyntaxHighlighter(LanguageVersion languageVersion) {
    myLanguageVersion = languageVersion;
  }

  @Nonnull
  @Override
  public Lexer getHighlightingLexer() {
    return getHighlightingLexer(myLanguageVersion);
  }

  public abstract Lexer getHighlightingLexer(LanguageVersion languageVersion);

  @Nonnull
  @Override
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    return getTokenHighlights(myLanguageVersion, tokenType);
  }

  @Nonnull
  public TextAttributesKey[] getTokenHighlights(LanguageVersion languageVersion, IElementType tokenType) {
    return EMPTY;
  }
}
