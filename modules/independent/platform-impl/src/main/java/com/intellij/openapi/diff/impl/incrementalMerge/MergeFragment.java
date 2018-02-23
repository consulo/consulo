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
package com.intellij.openapi.diff.impl.incrementalMerge;

import com.intellij.openapi.util.TextRange;
import javax.annotation.Nonnull;

final class MergeFragment {

  @Nonnull
  private final TextRange myLeft;
  @Nonnull
  private final TextRange myBase;
  @Nonnull
  private final TextRange myRight;

  MergeFragment(@Nonnull TextRange left, @Nonnull TextRange base, @Nonnull TextRange right) {
    myLeft = left;
    myBase = base;
    myRight = right;
  }

  @Nonnull
  TextRange getLeft() {
    return myLeft;
  }

  @Nonnull
  TextRange getBase() {
    return myBase;
  }

  @Nonnull
  TextRange getRight() {
    return myRight;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MergeFragment fragment = (MergeFragment)o;

    if (!myBase.equals(fragment.myBase)) return false;
    if (!myLeft.equals(fragment.myLeft)) return false;
    if (!myRight.equals(fragment.myRight)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = myLeft.hashCode();
    result = 31 * result + myBase.hashCode();
    result = 31 * result + myRight.hashCode();
    return result;
  }

  public String toString() {
    return "<" + myLeft.toString() + ", " + myBase.toString() + ", " + myRight.toString() + ">";
  }
}
