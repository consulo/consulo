/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.filter;

import consulo.versionControlSystem.log.VcsLogFilter;
import consulo.versionControlSystem.log.VcsLogRootFilter;
import consulo.versionControlSystem.log.VcsLogStructureFilter;
import org.jspecify.annotations.Nullable;

/*package private*/ class VcsLogFileFilter implements VcsLogFilter {
  private final @Nullable VcsLogStructureFilter myStructureFilter;
  private final @Nullable VcsLogRootFilter myRootFilter;

  public VcsLogFileFilter(@Nullable VcsLogStructureFilter structureFilter, @Nullable VcsLogRootFilter rootFilter) {
    myStructureFilter = structureFilter;
    myRootFilter = rootFilter;
  }

  public @Nullable VcsLogStructureFilter getStructureFilter() {
    return myStructureFilter;
  }

  public @Nullable VcsLogRootFilter getRootFilter() {
    return myRootFilter;
  }
}
