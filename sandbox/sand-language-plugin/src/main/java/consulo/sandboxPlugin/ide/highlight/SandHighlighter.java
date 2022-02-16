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
package consulo.sandboxPlugin.ide.highlight;

import consulo.codeEditor.colorScheme.TextAttributesKey;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.LanguageVersionableSyntaxHighlighter;
import consulo.language.lexer.Lexer;
import consulo.language.version.LanguageVersion;
import consulo.sandboxPlugin.lang.version.BaseSandLanguageVersion;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandHighlighter extends LanguageVersionableSyntaxHighlighter {
  public SandHighlighter(LanguageVersion languageVersion) {
    super(languageVersion);
  }

  @Override
  public Lexer getHighlightingLexer(LanguageVersion languageVersion) {
    BaseSandLanguageVersion sandLanguageVersion = (BaseSandLanguageVersion) languageVersion;
    return sandLanguageVersion.createLexer();
  }

  @Nonnull
  @Override
  public TextAttributesKey[] getTokenHighlights(LanguageVersion languageVersion, IElementType tokenType) {
    BaseSandLanguageVersion sandLanguageVersion = (BaseSandLanguageVersion) languageVersion;
    if(sandLanguageVersion.getHighlightKeywords().contains(tokenType)) {
      return pack(SandHighlighterKeys.KEYWORD);
    }
    else if(sandLanguageVersion.getCommentTokens().contains(tokenType)) {
      return pack(SandHighlighterKeys.LINE_COMMENT);
    }
    return EMPTY;
  }
}
