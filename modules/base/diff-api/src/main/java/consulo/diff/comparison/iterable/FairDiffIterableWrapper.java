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
package consulo.diff.comparison.iterable;

import consulo.diff.util.Range;
import jakarta.annotation.Nonnull;

import java.util.Iterator;

@SuppressWarnings("ConstantConditions")
class FairDiffIterableWrapper extends DiffIterableBase implements FairDiffIterable {
  @Nonnull
  private final DiffIterable myIterable;

  public FairDiffIterableWrapper(@Nonnull DiffIterable iterable) {
    myIterable = iterable;
  }

  @Override
  public int getLength1() {
    return myIterable.getLength1();
  }

  @Override
  public int getLength2() {
    return myIterable.getLength2();
  }

  @Override
  @Nonnull
  public Iterator<Range> changes() {
    return myIterable.changes();
  }

  @Override
  @Nonnull
  public Iterator<Range> unchanged() {
    return myIterable.unchanged();
  }
}
