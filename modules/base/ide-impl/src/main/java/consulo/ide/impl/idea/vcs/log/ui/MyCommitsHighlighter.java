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
package consulo.ide.impl.idea.vcs.log.ui;

import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogUserFilterImpl;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.util.VcsUserUtil;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class MyCommitsHighlighter implements VcsLogHighlighter {
  @Nonnull
  private final VcsLogData myLogData;
  @Nonnull
  private final VcsLogUi myLogUi;
  private boolean myShouldHighlightUser = false;

  public MyCommitsHighlighter(@Nonnull VcsLogData logData, @Nonnull VcsLogUi logUi) {
    myLogData = logData;
    myLogUi = logUi;
  }

  @Nonnull
  @Override
  public VcsCommitStyle getStyle(@Nonnull VcsShortCommitDetails details, boolean isSelected) {
    if (!myLogUi.isHighlighterEnabled(MyCommitsHighlighterFactory.ID)) return VcsCommitStyle.DEFAULT;
    if (myShouldHighlightUser) {
      VcsUser currentUser = myLogData.getCurrentUser().get(details.getRoot());
      if (currentUser != null && VcsUserUtil.isSamePerson(currentUser, details.getAuthor())) {
        return VcsCommitStyleFactory.bold();
      }
    }
    return VcsCommitStyle.DEFAULT;
  }

  @Override
  public void update(@Nonnull VcsLogDataPack dataPack, boolean refreshHappened) {
    myShouldHighlightUser = !isSingleUser() && !isFilteredByCurrentUser(dataPack.getFilters());
  }

  // returns true if only one user commits to this repository
  private boolean isSingleUser() {
    NotNullFunction<VcsUser, String> nameToString = user -> VcsUserUtil.getNameInStandardForm(VcsUserUtil.getShortPresentation(user));
    Set<String> allUserNames = ContainerUtil.newHashSet(ContainerUtil.map(myLogData.getAllUsers(), nameToString));
    Set<String> currentUserNames = ContainerUtil.newHashSet(ContainerUtil.map(myLogData.getCurrentUser().values(), nameToString));
    return allUserNames.size() == currentUserNames.size() && currentUserNames.containsAll(allUserNames);
  }

  // returns true if filtered by "me"
  private static boolean isFilteredByCurrentUser(@Nonnull VcsLogFilterCollection filters) {
    VcsLogUserFilter userFilter = filters.getUserFilter();
    if (userFilter == null) return false;
    Collection<String> filterByName = ((VcsLogUserFilterImpl)userFilter).getUserNamesForPresentation();
    if (Collections.singleton(VcsLogUserFilterImpl.ME).containsAll(filterByName)) return true;
    return false;
  }
}
