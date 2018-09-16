package com.intellij.psi.templateLanguages;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.psi.tree.IElementType;
import javax.annotation.Nonnull;

import static com.intellij.psi.TokenType.BAD_CHARACTER;

/**
* @author peter
*/
public class TemplateDataHighlighterWrapper implements SyntaxHighlighter {
  private final SyntaxHighlighter myHighlighter;

  public TemplateDataHighlighterWrapper(SyntaxHighlighter highlighter) {
    myHighlighter = highlighter;
  }

  @Override
  @Nonnull
  public Lexer getHighlightingLexer() {
    return myHighlighter.getHighlightingLexer();
  }

  @Override
  @Nonnull
  public TextAttributesKey[] getTokenHighlights(final IElementType tokenType) {
    if (tokenType == BAD_CHARACTER) {
      return new TextAttributesKey[0];
    }

    return myHighlighter.getTokenHighlights(tokenType);
  }
}
