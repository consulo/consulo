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
package consulo.language.pattern;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class IElementTypePattern extends ObjectPattern<IElementType, IElementTypePattern> {
  protected IElementTypePattern() {
    super(IElementType.class);
  }

  public IElementTypePattern or(@Nonnull final IElementType... types){
    return tokenSet(TokenSet.create(types));
  }

  public IElementTypePattern tokenSet(@Nonnull final TokenSet tokenSet){
    return with(new PatternCondition<IElementType>("tokenSet") {
      @Override
      public boolean accepts(@Nonnull final IElementType type, final ProcessingContext context) {
        return tokenSet.contains(type);
      }
    });
  }

}
