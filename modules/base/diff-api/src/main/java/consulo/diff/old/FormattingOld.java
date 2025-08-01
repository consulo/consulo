/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.diff.old;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.TestOnly;

@Deprecated
public class FormattingOld extends Word {
  @TestOnly
  public FormattingOld(@Nonnull String baseText, @Nonnull TextRange range) {
    this(DiffString.create(baseText), range);
  }

  public FormattingOld(@Nonnull DiffString text, @Nonnull TextRange range) {
    super(text, range);
  }

  public int hashCode() {
    return -1;
  }

  public boolean equals(Object obj) {
    return obj instanceof FormattingOld;
  }

  @Override
  public boolean isWhitespace() {
    return true;
  }
}
