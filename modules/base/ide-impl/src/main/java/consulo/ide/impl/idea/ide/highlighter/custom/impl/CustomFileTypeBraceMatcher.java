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

package consulo.ide.impl.idea.ide.highlighter.custom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.BracePair;
import consulo.language.Language;
import consulo.language.PairedBraceMatcher;
import consulo.language.ast.IElementType;
import consulo.language.editor.internal.PairedBraceMatcherAdapter;
import consulo.language.plain.PlainTextLanguage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static consulo.language.internal.custom.CustomHighlighterTokenType.*;

/**
 * @author Maxim.Mossienko
 */
@ExtensionImpl
public class CustomFileTypeBraceMatcher implements PairedBraceMatcher {
  public static final BracePair[] PAIRS = new BracePair[]{
          new BracePair(L_BRACKET, R_BRACKET, true),
          new BracePair(L_ANGLE, R_ANGLE, true),
          new BracePair(L_PARENTH, R_PARENTH, true),
          new BracePair(L_BRACE, R_BRACE, true),
  };

  @Override
  public BracePair[] getPairs() {
    return PAIRS;
  }

  @Override
  public boolean isPairedBracesAllowedBeforeType(@Nonnull final IElementType lbraceType, @Nullable final IElementType contextType) {
    return contextType == PUNCTUATION ||
           contextType == WHITESPACE ||
           isRBraceToken(contextType);
  }

  private static boolean isRBraceToken(IElementType type) {
    for (BracePair pair : PAIRS) {
      if (type == pair.getRightBraceType()) return true;
    }
    return false;
  }

  @Nonnull
  public static PairedBraceMatcherAdapter createBraceMatcher() {
    return new PairedBraceMatcherAdapter(new CustomFileTypeBraceMatcher(), IDENTIFIER.getLanguage()) {
      @Override
      public int getBraceTokenGroupId(IElementType tokenType) {
        int id = super.getBraceTokenGroupId(tokenType);
        return id == -1 ? -1 : 777;
      }
    };
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return PlainTextLanguage.INSTANCE;
  }
}
