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
package consulo.ide.impl.idea.openapi.diff.impl.highlighting;

import consulo.logging.Logger;
import consulo.ide.impl.idea.openapi.diff.impl.string.DiffString;
import consulo.ide.impl.idea.openapi.diff.ex.DiffFragment;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public enum FragmentSide {

  SIDE1(0, 0) {
    @Override
    @Nullable
    public DiffString getText(@Nonnull DiffFragment fragment) {
      return fragment.getText1();
    }

    @Override
    @Nonnull
    protected DiffFragment createDiffFragment(@Nullable DiffString text, @Nullable DiffString otherText) {
      return new DiffFragment(text, otherText);
    }

    @Override
    public FragmentSide otherSide() {
      return SIDE2;
    }
  },

  SIDE2(1, 2) {
    @Override
    @Nullable
    public DiffString getText(@Nonnull DiffFragment fragment) {
      return fragment.getText2();
    }

    @Override
    @Nonnull
    protected DiffFragment createDiffFragment(@Nullable DiffString text, @Nullable DiffString otherText) {
      return new DiffFragment(otherText, text);
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
  public DiffFragment createFragment(@Nullable DiffString text, @Nullable DiffString otherText, boolean modified) {
    DiffFragment fragment = createDiffFragment(text, otherText);
    if (!fragment.isOneSide()) fragment.setModified(modified);
    return fragment;
  }

  @Nullable
  public abstract DiffString getText(@Nonnull DiffFragment fragment);
  public abstract FragmentSide otherSide();
  @Nonnull
  protected abstract DiffFragment createDiffFragment(@Nullable DiffString text, @Nullable DiffString otherText);

  public int getIndex() {
    return myIndex;
  }

  public int getMergeIndex() {
    return myMergeIndex;
  }

  @Nullable
  public DiffString getOtherText(@Nonnull DiffFragment fragment) {
    return otherSide().getText(fragment);
  }

  public IllegalArgumentException invalidException() {
    return new IllegalArgumentException(String.valueOf(this));
  }

  public static FragmentSide chooseSide(DiffFragment oneSide) {
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
