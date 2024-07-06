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

import consulo.diff.fragment.LineFragment;
import consulo.diff.util.LineRange;
import jakarta.annotation.Nonnull;

public class ChangedBlock {
  private final int myLine1;
  private final int myLine2;

  @Nonnull
  private final LineRange myRange1;
  @Nonnull
  private final LineRange myRange2;

  @Nonnull
  private final LineFragment myLineFragment;

  public ChangedBlock(int line1,
                      int line2,
                      @Nonnull LineRange range1,
                      @Nonnull LineRange range2,
                      @Nonnull LineFragment lineFragment) {
    myLine1 = line1;
    myLine2 = line2;
    myRange1 = range1;
    myRange2 = range2;
    myLineFragment = lineFragment;
  }

  @Nonnull
  public LineRange getRange1() {
    return myRange1;
  }

  @Nonnull
  public LineRange getRange2() {
    return myRange2;
  }

  public int getLine1() {
    return myLine1;
  }

  public int getLine2() {
    return myLine2;
  }

  @Nonnull
  public LineFragment getLineFragment() {
    return myLineFragment;
  }
}
