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
package consulo.sandboxPlugin.lang.parser;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderUtil;
import consulo.language.parser.PsiParser;
import consulo.language.version.LanguageVersion;
import consulo.sandboxPlugin.lang.psi.SandElements;
import consulo.sandboxPlugin.lang.psi.SandTokens;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandParser implements PsiParser {
  private List<Pair<IElementType, IElementType>> myPairs;

  public SandParser(List<Pair<IElementType, IElementType>> list) {
    myPairs = list;
  }

  @Nonnull
  @Override
  public ASTNode parse(@Nonnull IElementType root, @Nonnull PsiBuilder builder, @Nonnull LanguageVersion languageVersion) {
    PsiBuilder.Marker mark = builder.mark();
    while (!builder.eof()) {
      boolean find = false;
      for (Pair<IElementType, IElementType> pair : myPairs) {
        if (builder.getTokenType() == pair.getFirst()) {
          PsiBuilder.Marker defMark = builder.mark();
          builder.advanceLexer();

          if (!PsiBuilderUtil.expect(builder, SandTokens.IDENTIFIER)) {
            builder.error("Identifier expected");
          }

          PsiBuilderUtil.expect(builder, SandTokens.LBRACE);

          while (builder.getTokenType() == SandTokens.STRING_LITERAL) {
            PsiBuilder.Marker stringExp = builder.mark();
            builder.advanceLexer();
            stringExp.done(SandElements.STRING_EXPRESSION);
          }
          
          PsiBuilderUtil.expect(builder, SandTokens.RBRACE);

          defMark.done(pair.getSecond());
          find = true;
        }
      }

      if (!find) {
        builder.error("Expected start token");
        builder.advanceLexer();
      }
    }
    mark.done(root);
    return builder.getTreeBuilt();
  }
}
