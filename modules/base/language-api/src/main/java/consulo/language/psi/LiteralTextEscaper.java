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

package consulo.language.psi;

import consulo.document.util.TextRange;

import jakarta.annotation.Nonnull;

/**
 * @author cdr
*/
public abstract class LiteralTextEscaper<T extends PsiLanguageInjectionHost> {
  protected final T myHost;

  protected LiteralTextEscaper(@Nonnull T host) {
    myHost = host;
  }

  public abstract boolean decode(@Nonnull TextRange rangeInsideHost, @Nonnull StringBuilder outChars);

  /**
   * @param offsetInDecoded offset in the parsed injected file
   * @param rangeInsideHost range where injection is performed,
   *   E.g. if some language fragment xyz was injected into string literal expression "xyz", then rangeInsideHost = (1,4)
   * @return offset in the host PSI element, or -1 if offset is out of host range.
   * E.g. if some language fragment xyz was injected into string literal expression "xyz", then
   *     getOffsetInHost(0)==1 (there is an 'x' at offset 0 in injected fragment,
   *                            and that 'x' occurs in "xyz" string literal at offset 1
   *                            since string literal expression "xyz" starts with double quote)
   *     getOffsetInHost(1)==2
   *     getOffsetInHost(2)==3
   *     getOffsetInHost(3)==-1  (out of range)
   *
   * Similarly, for some language fragment xyz being injected into xml text inside xml tag 'tag': <tag>xyz</tag>
   *     getOffsetInHost(0)==0 (there is an 'x' at offset 0 in injected fragment,
   *                            and that 'x' occurs in xyz xml text at offset 0)
   *     getOffsetInHost(1)==1
   *     getOffsetInHost(2)==2
   *     getOffsetInHost(3)==-1  (out of range)
   */
  public abstract int getOffsetInHost(int offsetInDecoded, @Nonnull TextRange rangeInsideHost);

  @Nonnull
  public TextRange getRelevantTextRange() {
    return TextRange.from(0, myHost.getTextLength());
  }

  public abstract boolean isOneLine();

  public static <T extends PsiLanguageInjectionHost> LiteralTextEscaper<T> createSimple(T element) {
    return new LiteralTextEscaper<T>(element) {
      @Override
      public boolean decode(@Nonnull TextRange rangeInsideHost, @Nonnull StringBuilder outChars) {
        outChars.append(rangeInsideHost.substring(myHost.getText()));
        return true;
      }

      @Override
      public int getOffsetInHost(int offsetInDecoded, @Nonnull TextRange rangeInsideHost) {
        return rangeInsideHost.getStartOffset() + offsetInDecoded;
      }

      @Override
      public boolean isOneLine() {
        return true;
      }
    };
  }
}
