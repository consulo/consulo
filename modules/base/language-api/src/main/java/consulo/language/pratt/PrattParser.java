/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.pratt;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiParser;
import consulo.language.version.LanguageVersion;

import jakarta.annotation.Nonnull;

/**
 * @author peter
*/
public abstract class PrattParser implements PsiParser {
  protected abstract PrattRegistry getRegistry();

  @Override
  @Nonnull
  public final ASTNode parse(@Nonnull IElementType root, @Nonnull PsiBuilder builder, @Nonnull LanguageVersion languageVersion) {
    PrattBuilder prattBuilder = PrattBuilderImpl.createBuilder(builder, getRegistry());
    MutableMarker marker = prattBuilder.mark();
    parse(prattBuilder);
    marker.finish(root);
    return builder.getTreeBuilt();
  }

  protected void parse(PrattBuilder builder) {
    builder.parse();
    while (!builder.isEof()) builder.advance();
  }
}
