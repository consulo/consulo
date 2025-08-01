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

import consulo.application.util.diff.FilesTooBigForDiffException;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;

@Deprecated
public interface DiffPolicyOld {
  @Nonnull
  DiffFragmentOld[] buildFragments(@Nonnull DiffString text1, @Nonnull DiffString text2) throws FilesTooBigForDiffException;

  @Nonnull
  @TestOnly
  DiffFragmentOld[] buildFragments(@Nonnull String text1, @Nonnull String text2) throws FilesTooBigForDiffException;

  DiffPolicyOld LINES_WO_FORMATTING = new LineBlocks(ComparisonPolicyOld.IGNORE_SPACE);
  DiffPolicyOld DEFAULT_LINES = new LineBlocks(ComparisonPolicyOld.DEFAULT);

  class LineBlocks implements DiffPolicyOld {
    private final ComparisonPolicyOld myComparisonPolicy;

    public LineBlocks(ComparisonPolicyOld comparisonPolicy) {
      myComparisonPolicy = comparisonPolicy;
    }

    @Override
    @Nonnull
    @TestOnly
    public DiffFragmentOld[] buildFragments(@Nonnull String text1, @Nonnull String text2) throws FilesTooBigForDiffException {
      return buildFragments(DiffString.create(text1), DiffString.create(text2));
    }

    @Nonnull
    @Override
    public DiffFragmentOld[] buildFragments(@Nonnull DiffString text1, @Nonnull DiffString text2) throws FilesTooBigForDiffException {
      DiffString[] strings1 = text1.tokenize();
      DiffString[] strings2 = text2.tokenize();
      return myComparisonPolicy.buildDiffFragmentsFromLines(strings1, strings2);
    }

  }

  class ByChar implements DiffPolicyOld {
    private final ComparisonPolicyOld myComparisonPolicy;

    public ByChar(ComparisonPolicyOld comparisonPolicy) {
      myComparisonPolicy = comparisonPolicy;
    }

    @Override
    @Nonnull
    @TestOnly
    public DiffFragmentOld[] buildFragments(@Nonnull String text1, @Nonnull String text2) throws FilesTooBigForDiffException {
      return buildFragments(DiffString.create(text1), DiffString.create(text2));
    }

    @Nonnull
    @Override
    public DiffFragmentOld[] buildFragments(@Nonnull DiffString text1, @Nonnull DiffString text2) throws FilesTooBigForDiffException {
      return myComparisonPolicy.buildFragments(splitByChar(text1), splitByChar(text2));
    }

    private static DiffString[] splitByChar(@Nonnull DiffString text) {
      DiffString[] result = new DiffString[text.length()];
      for (int i = 0; i < result.length; i++) {
        result[i] = text.substring(i, i + 1);
      }
      return result;
    }
  }

}
