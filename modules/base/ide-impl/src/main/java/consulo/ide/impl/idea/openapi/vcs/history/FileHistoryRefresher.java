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
package consulo.ide.impl.idea.openapi.vcs.history;

import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.history.VcsHistoryProviderEx;
import consulo.versionControlSystem.history.VcsHistoryProvider;
import consulo.versionControlSystem.history.VcsRevisionNumber;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Refreshes file history.
 * @author irengrig
 * @author Kirill Likhodedov
 */
public class FileHistoryRefresher implements FileHistoryRefresherI {
  private final FileHistorySessionPartner mySessionPartner;
  private final VcsHistoryProvider myVcsHistoryProvider;
  private final FilePath myPath;
  private final AbstractVcs myVcs;
  @Nullable private final VcsRevisionNumber myStartingRevisionNumber;
  private boolean myCanUseCache;
  private boolean myIsRefresh;

  public FileHistoryRefresher(VcsHistoryProvider vcsHistoryProvider,
                              FilePath path,
                              AbstractVcs vcs) {
    this(vcsHistoryProvider, path, null, vcs);
  }

  public FileHistoryRefresher(VcsHistoryProviderEx vcsHistoryProvider,
                              FilePath path,
                              @jakarta.annotation.Nullable VcsRevisionNumber startingRevisionNumber,
                              AbstractVcs vcs) {
    this((VcsHistoryProvider)vcsHistoryProvider, path, startingRevisionNumber, vcs);
  }

  private FileHistoryRefresher(VcsHistoryProvider vcsHistoryProvider,
                               FilePath path,
                               @Nullable VcsRevisionNumber startingRevisionNumber,
                               AbstractVcs vcs) {
    myVcsHistoryProvider = vcsHistoryProvider;
    myPath = path;
    myVcs = vcs;
    myStartingRevisionNumber = startingRevisionNumber;
    mySessionPartner = new FileHistorySessionPartner(vcsHistoryProvider, path, startingRevisionNumber, vcs, this);
    myCanUseCache = true;
  }

  @Nonnull
  public static FileHistoryRefresherI findOrCreate(@Nonnull VcsHistoryProvider vcsHistoryProvider,
                                                   @Nonnull FilePath path,
                                                   @Nonnull AbstractVcs vcs) {
    FileHistoryRefresherI refresher = FileHistorySessionPartner.findExistingHistoryRefresher(vcs.getProject(), path, null);
    return refresher == null ? new FileHistoryRefresher(vcsHistoryProvider, path, vcs) : refresher;
  }

  @Nonnull
  public static FileHistoryRefresherI findOrCreate(@Nonnull VcsHistoryProviderEx vcsHistoryProvider,
                                                   @Nonnull FilePath path,
                                                   @Nonnull AbstractVcs vcs,
                                                   @Nullable VcsRevisionNumber startingRevisionNumber) {
    FileHistoryRefresherI refresher = FileHistorySessionPartner.findExistingHistoryRefresher(vcs.getProject(), path, startingRevisionNumber);
    return refresher == null ? new FileHistoryRefresher(vcsHistoryProvider, path, startingRevisionNumber, vcs) : refresher;
  }

  /**
   * @param canUseLastRevision
   */
  @Override
  public void run(boolean isRefresh, boolean canUseLastRevision) {
    myIsRefresh = isRefresh;
    mySessionPartner.beforeRefresh();
    VcsHistoryProviderBackgroundableProxy proxy = new VcsHistoryProviderBackgroundableProxy(myVcs, myVcsHistoryProvider,
                                                                                            myVcs.getDiffProvider());
    VcsKey key = myVcs.getKeyInstanceMethod();
    if (myVcsHistoryProvider instanceof VcsHistoryProviderEx && myStartingRevisionNumber != null) {
      proxy.executeAppendableSession(key, myPath, myStartingRevisionNumber, mySessionPartner, null);
    }
    else {
      proxy.executeAppendableSession(key, myPath, mySessionPartner, null, myCanUseCache, canUseLastRevision);
    }
    myCanUseCache = false;
  }

  /**
   * Was the refresher called for the first time or via refresh.
   * @return
   */
  @Override
  public boolean isFirstTime() {
    return !myIsRefresh;
  }
}
