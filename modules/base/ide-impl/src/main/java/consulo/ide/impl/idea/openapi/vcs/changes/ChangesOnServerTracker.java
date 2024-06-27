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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsListener;
import consulo.versionControlSystem.impl.internal.change.PlusMinus;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.change.Change;

import java.util.Collection;

public interface ChangesOnServerTracker extends PlusMinus<Pair<String, AbstractVcs>>, VcsListener {
  // todo add vcs parameter???
  void invalidate(final Collection<String> paths);
  boolean isUpToDate(final Change change);
  boolean updateStep();
}
