/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.internal;

import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenSeparatorGenerator;
import consulo.language.ast.TokenType;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiUtilCore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11-Jul-22
 */
public class DefaultTokenSeparatorGenerator implements TokenSeparatorGenerator {
  @Nullable
  @Override
  public ASTNode generateWhitespaceBetweenTokens(ASTNode left, ASTNode right) {
    Language l = PsiUtilCore.getNotAnyLanguage(left);
    Language rightLang = PsiUtilCore.getNotAnyLanguage(right);
    if (rightLang.isKindOf(l)) {
      l = rightLang; // get more precise lexer
    }
    final ParserDefinition parserDefinition = ParserDefinition.forLanguage(l);
    if (parserDefinition != null) {
      PsiManager manager = right.getTreeParent().getPsi().getManager();
      ASTNode generatedWhitespace;
      switch (parserDefinition.spaceExistenceTypeBetweenTokens(left, right)) {
        case MUST:
          generatedWhitespace = ASTElementFactory.getInstance(manager.getProject()).createSingleLeafElement(TokenType.WHITE_SPACE, " ", 0, 1, null, manager);
          break;
        case MUST_LINE_BREAK:
          generatedWhitespace = ASTElementFactory.getInstance(manager.getProject()).createSingleLeafElement(TokenType.WHITE_SPACE, "\n", 0, 1, null, manager);
          break;
        default:
          generatedWhitespace = null;
      }
      return generatedWhitespace;
    }
    return null;
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}
