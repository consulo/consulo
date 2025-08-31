package consulo.language.editor.highlight;

import consulo.colorScheme.TextAttributesKey;
import consulo.language.ast.IElementType;
import consulo.language.lexer.Lexer;

import jakarta.annotation.Nonnull;

import static consulo.language.ast.TokenType.BAD_CHARACTER;

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
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    if (tokenType == BAD_CHARACTER) {
      return new TextAttributesKey[0];
    }

    return myHighlighter.getTokenHighlights(tokenType);
  }
}
