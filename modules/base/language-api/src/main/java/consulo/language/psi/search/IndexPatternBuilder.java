// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.psi.search;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.stub.todo.LexerBasedTodoIndexer;
import consulo.language.lexer.Lexer;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 * @see LexerBasedTodoIndexer
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface IndexPatternBuilder {
  ExtensionPointName<IndexPatternBuilder> EP_NAME = ExtensionPointName.create(IndexPatternBuilder.class);

  @Nullable
  Lexer getIndexingLexer(@Nonnull PsiFile file);

  @Nullable
  TokenSet getCommentTokenSet(@Nonnull PsiFile file);

  int getCommentStartDelta(IElementType tokenType);

  int getCommentEndDelta(IElementType tokenType);

  /**
   * Characters (in addition to whitespace) which can be present in the indent section of pattern occurrence's continuation
   * on subsequent line
   */
  @Nonnull
  default String getCharsAllowedInContinuationPrefix(@Nonnull IElementType tokenType) {
    return "";
  }

  default int getCommentStartDelta(@Nonnull IElementType tokenType, @Nonnull CharSequence tokenText) {
    return getCommentStartDelta(tokenType);
  }
}
