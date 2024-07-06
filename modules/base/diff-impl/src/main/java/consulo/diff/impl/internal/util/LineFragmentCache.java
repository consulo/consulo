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
package consulo.diff.impl.internal.util;

import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.fragment.LineFragment;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineFragmentCache {
  private final long myModificationStamp1;
  private final long myModificationStamp2;

  @Nonnull
  private final Map<ComparisonPolicy, PolicyData> myFragments;

  public LineFragmentCache(@Nonnull LineFragmentCache cache) {
    myModificationStamp1 = cache.myModificationStamp1;
    myModificationStamp2 = cache.myModificationStamp2;

    myFragments = new HashMap<ComparisonPolicy, PolicyData>(3);
    for (Map.Entry<ComparisonPolicy, PolicyData> entry : cache.myFragments.entrySet()) {
      myFragments.put(entry.getKey(), entry.getValue());
    }
  }

  public LineFragmentCache(long modificationStamp1,
                           long modificationStamp2) {
    myModificationStamp1 = modificationStamp1;
    myModificationStamp2 = modificationStamp2;
    myFragments = new HashMap<ComparisonPolicy, PolicyData>(3);
  }

  public boolean checkStamps(long stamp1, long stamp2) {
    return myModificationStamp1 == stamp1 && myModificationStamp2 == stamp2;
  }

  @Nullable
  public PolicyData getData(@Nonnull ComparisonPolicy policy) {
    return myFragments.get(policy);
  }

  public void putData(@Nonnull ComparisonPolicy policy, @Nonnull List<LineFragment> fragments, boolean isInnerFragments) {
    myFragments.put(policy, new PolicyData(fragments, isInnerFragments));
  }

  public static class PolicyData {
    @Nonnull
    private final List<LineFragment> myFragments;
    private final boolean myInnerFragments;

    public PolicyData(@Nonnull List<LineFragment> fragments, boolean innerFragments) {
      myFragments = fragments;
      myInnerFragments = innerFragments;
    }

    @Nonnull
    public List<LineFragment> getFragments() {
      return myFragments;
    }

    public boolean isInnerFragments() {
      return myInnerFragments;
    }
  }
}
