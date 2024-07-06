/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.fragment;

import consulo.diff.util.Side;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

public class HighlightRange {
  @Nonnull
  private final TextRange myBase;
  @Nonnull
  private final TextRange myChanged;
  @Nonnull
  private final Side mySide;

  public HighlightRange(@Nonnull Side side, @Nonnull TextRange base, @Nonnull TextRange changed) {
    mySide = side;
    myBase = base;
    myChanged = changed;
  }

  @Nonnull
  public Side getSide() {
    return mySide;
  }

  @Nonnull
  public TextRange getBase() {
    return myBase;
  }

  @Nonnull
  public TextRange getChanged() {
    return myChanged;
  }
}
