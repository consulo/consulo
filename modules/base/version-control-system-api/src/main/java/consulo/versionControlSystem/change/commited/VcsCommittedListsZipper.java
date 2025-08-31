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
package consulo.versionControlSystem.change.commited;

import consulo.util.lang.Pair;
import consulo.versionControlSystem.RepositoryLocation;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;

import java.util.List;

public interface VcsCommittedListsZipper {
  Pair<List<RepositoryLocationGroup>, List<RepositoryLocation>> groupLocations(List<RepositoryLocation> in);

  CommittedChangeList zip(RepositoryLocationGroup group, List<CommittedChangeList> lists);

  long getNumber(CommittedChangeList list);
}
