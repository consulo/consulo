/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.diff.util;

import javax.annotation.Nonnull;

public class MergeConflictType {
  @Nonnull
  private final TextDiffType myType;
  private final boolean myLeftChange;
  private final boolean myRightChange;

  public MergeConflictType(@Nonnull TextDiffType type) {
    this(type, true, true);
  }

  public MergeConflictType(@Nonnull TextDiffType type, boolean leftChange, boolean rightChange) {
    myType = type;
    myLeftChange = leftChange;
    myRightChange = rightChange;
  }

  @Nonnull
  public TextDiffType getDiffType() {
    return myType;
  }

  public boolean isChange(@Nonnull Side side) {
    return side.isLeft() ? myLeftChange : myRightChange;
  }

  public boolean isChange(@Nonnull ThreeSide side) {
    switch (side) {
      case LEFT:
        return myLeftChange;
      case BASE:
        return true;
      case RIGHT:
        return myRightChange;
      default:
        throw new IllegalArgumentException(side.toString());
    }
  }
}
