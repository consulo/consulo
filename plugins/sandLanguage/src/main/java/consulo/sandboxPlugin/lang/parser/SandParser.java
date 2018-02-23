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

import com.intellij.lang.*;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import consulo.lang.LanguageVersion;
import javax.annotation.Nonnull;
import consulo.sandboxPlugin.lang.psi.SandTokens;

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
          defMark.done(pair.getSecond());
          find = true;
        }
      }

      if(!find) {
        builder.error("Expected start token");
        builder.advanceLexer();
      }
    }
    mark.done(root);
    return builder.getTreeBuilt();
  }
}
