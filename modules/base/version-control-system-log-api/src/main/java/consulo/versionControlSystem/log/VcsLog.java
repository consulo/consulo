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
package consulo.versionControlSystem.log;

import consulo.application.progress.ProgressIndicator;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Use this interface to access information available in the VCS Log.
 */
public interface VcsLog {
  Key<VcsLog> KEY = Key.create(VcsLog.class);

  /**
   * Returns commits currently selected in the log.
   */
  List<CommitId> getSelectedCommits();

  /**
   * Returns those details of the selected commit which are visible in the table. <br/>
   * The information for these commits are loaded fast (while scrolling),
   * and - which is more important - user is not likely to select anything unless he sees what he selects,
   * which means that the short details are already loaded. <br/>
   * This makes this method preferable to {@link #getSelectedDetails()}.
   */
  List<VcsShortCommitDetails> getSelectedShortDetails();

  /**
   * Returns details of the selected commits.
   * For commits that are not loaded an instance of LoadingDetails is returned.
   */
  List<VcsFullCommitDetails> getSelectedDetails();

  /**
   * Sends a request to load details that are currently selected.
   * Details are loaded in background. If a progress indicator is specified it is used during loading process.
   * After all details are loaded they are provided to the consumer in the EDT.
   *
   * @param consumer  called in EDT after all details are loaded.
   * @param indicator progress indicator to use in loading process, can be null.
   */
  void requestSelectedDetails(Consumer<List<VcsFullCommitDetails>> consumer, @Nullable ProgressIndicator indicator);

  /**
   * Returns names of branches which contain the given commit, or null if this information is unavailable.
   */
  @Nullable Collection<String> getContainingBranches(Hash commitHash, VirtualFile root);

  /**
   * Asynchronously selects the commit node defined by the given reference (commit hash, branch or tag).
   * Returns a {@link Future future} that allows to check if the commit was selected, wait for the selection while log is being loaded,
   * or cancel commit selection.
   */
  Future<Boolean> jumpToReference(String reference);

  /**
   * Returns {@link VcsLogProvider VcsLogProviders} which are active in this log, i.e. which VCS roots are shown in the log.
   */
  Map<VirtualFile, VcsLogProvider> getLogProviders();
}
