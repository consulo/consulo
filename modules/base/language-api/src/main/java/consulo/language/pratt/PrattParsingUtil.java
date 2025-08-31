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
package consulo.language.pratt;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class PrattParsingUtil {
  private PrattParsingUtil() {
  }

  public static void searchFor(PrattBuilder builder, @Nonnull PrattTokenType... types) {
    searchFor(builder, true, types);
  }

  public static boolean searchFor(PrattBuilder builder, boolean consume, PrattTokenType... types) {
    TokenSet set = TokenSet.create(types);
    if (!set.contains(builder.getTokenType())) {
      builder.assertToken(types[0]);
      while (!set.contains(builder.getTokenType()) && !builder.isEof()) {
        builder.advance();
      }
    }
    if (consume) {
      builder.advance();
    }
    return !builder.isEof();
  }

  @Nullable
  public static IElementType parseOption(PrattBuilder builder, int rightPriority) {
    MutableMarker marker = builder.mark();
    IElementType type = builder.createChildBuilder(rightPriority).parse();
    if (type == null) {
      marker.rollback();
    } else {
      marker.finish();
    }
    return type;
  }
}
