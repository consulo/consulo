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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.versionControlSystem.log.VcsLogDetailsFilter;
import consulo.versionControlSystem.log.impl.internal.data.index.VcsLogIndex;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Set;

public class EmptyIndex implements VcsLogIndex {
  @Override
  public void scheduleIndex(boolean full) {
  }

  @Override
  public boolean isIndexed(int commit) {
    return false;
  }

  @Override
  public boolean isIndexed(@Nonnull VirtualFile root) {
    return false;
  }

  @Override
  public void markForIndexing(int commit, @Nonnull VirtualFile root) {
  }

  @Override
  public boolean canFilter(@Nonnull List<VcsLogDetailsFilter> filters) {
    return false;
  }

  @Nonnull
  @Override
  public Set<Integer> filter(@Nonnull List<VcsLogDetailsFilter> detailsFilters) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public String getFullMessage(int index) {
    return null;
  }

  @Override
  public void markCorrupted() {
  }
}
