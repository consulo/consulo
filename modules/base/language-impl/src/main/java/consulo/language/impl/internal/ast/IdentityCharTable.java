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
package consulo.language.impl.internal.ast;

import consulo.language.util.CharTable;
import jakarta.annotation.Nonnull;

/**
 * @author max
 * @since 2006-09-16
 */
public class IdentityCharTable implements CharTable {
  private IdentityCharTable() { }

  public static final IdentityCharTable INSTANCE = new IdentityCharTable();

  @Nonnull
  @Override
  public CharSequence intern(@Nonnull CharSequence text) {
    return text;
  }

  @Nonnull
  @Override
  public CharSequence intern(@Nonnull CharSequence baseText, int startOffset, int endOffset) {
    if (endOffset - startOffset == baseText.length()) return baseText.toString();
    return baseText.subSequence(startOffset, endOffset);
  }
}
