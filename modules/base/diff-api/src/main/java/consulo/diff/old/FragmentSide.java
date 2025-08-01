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
package consulo.diff.old;

import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@Deprecated
public enum FragmentSide {

  SIDE1(0, 0) {
    @Override
    @Nullable
    public DiffString getText(@Nonnull DiffFragmentOld fragment) {
      return fragment.getText1();
    }

    @Override
    @Nonnull
    protected DiffFragmentOld createDiffFragment(@Nullable DiffString text, @Nullable DiffString otherText) {
      return new DiffFragmentOld(text, otherText);
    }

    @Override
    public FragmentSide otherSide() {
      return SIDE2;
    }
  },

  SIDE2(1, 2) {
    @Override
    @Nullable
    public DiffString getText(@Nonnull DiffFragmentOld fragment) {
      return fragment.getText2();
    }

    @Override
    @Nonnull
    protected DiffFragmentOld createDiffFragment(@Nullable DiffString text, @Nullable DiffString otherText) {
      return new DiffFragmentOld(otherText, text);
    }

    @Override
    public FragmentSide otherSide() {
      return SIDE1;
    }
  };

  private static final Logger LOG = Logger.getInstance(FragmentSide.class);

  private final int myIndex;
  private final int myMergeIndex;

  FragmentSide(int index, int mergeIndex) {
    myIndex = index;
    myMergeIndex = mergeIndex;
  }

  @Nonnull
  public DiffFragmentOld createFragment(@Nullable DiffString text, @Nullable DiffString otherText, boolean modified) {
    DiffFragmentOld fragment = createDiffFragment(text, otherText);
    if (!fragment.isOneSide()) fragment.setModified(modified);
    return fragment;
  }

  @Nullable
  public abstract DiffString getText(@Nonnull DiffFragmentOld fragment);
  public abstract FragmentSide otherSide();
  @Nonnull
  protected abstract DiffFragmentOld createDiffFragment(@Nullable DiffString text, @Nullable DiffString otherText);

  public int getIndex() {
    return myIndex;
  }

  public int getMergeIndex() {
    return myMergeIndex;
  }

  @Nullable
  public DiffString getOtherText(@Nonnull DiffFragmentOld fragment) {
    return otherSide().getText(fragment);
  }

  public IllegalArgumentException invalidException() {
    return new IllegalArgumentException(String.valueOf(this));
  }

  public static FragmentSide chooseSide(DiffFragmentOld oneSide) {
    LOG.assertTrue(oneSide.isOneSide());
    return oneSide.getText1() == null ? SIDE2 : SIDE1;
  }

  @Nonnull
  public static FragmentSide fromIndex(int index) {
    for (FragmentSide side : FragmentSide.values()) {
      if (side.getIndex() == index) {
        return side;
      }
    }
    throw new IllegalArgumentException(String.valueOf(index));
  }

}
