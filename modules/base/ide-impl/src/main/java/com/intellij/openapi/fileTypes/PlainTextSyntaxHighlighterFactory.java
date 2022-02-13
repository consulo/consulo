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
package com.intellij.openapi.fileTypes;

import com.intellij.ide.highlighter.custom.AbstractCustomLexer;
import com.intellij.ide.highlighter.custom.tokens.*;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.lexer.Lexer;
import consulo.language.lexer.MergingLexerAdapter;
import consulo.editor.colorScheme.TextAttributesKey;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.CustomHighlighterTokenType;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import javax.annotation.Nonnull;

import java.util.ArrayList;

/**
 * @author peter
 */
public class PlainTextSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
  @Override
  @Nonnull
  public SyntaxHighlighter getSyntaxHighlighter(final Project project, final VirtualFile virtualFile) {
    return new SyntaxHighlighterBase() {
      @Nonnull
      @Override
      public Lexer getHighlightingLexer() {
        return createPlainTextLexer();
      }

      @Nonnull
      @Override
      public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return EMPTY;
      }
    };
  }

  public static Lexer createPlainTextLexer() {
    ArrayList<TokenParser> tokenParsers = new ArrayList<TokenParser>();
    tokenParsers.add(new WhitespaceParser());

    tokenParsers.addAll(BraceTokenParser.getBraces());
    tokenParsers.addAll(BraceTokenParser.getParens());
    tokenParsers.addAll(BraceTokenParser.getBrackets());
    tokenParsers.addAll(BraceTokenParser.getAngleBrackets());

    return new MergingLexerAdapter(new AbstractCustomLexer(tokenParsers), TokenSet.create(CustomHighlighterTokenType.CHARACTER));
  }
}