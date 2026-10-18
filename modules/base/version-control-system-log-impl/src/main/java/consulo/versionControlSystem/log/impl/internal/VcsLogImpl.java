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
package consulo.versionControlSystem.log.impl.internal;

import com.google.common.util.concurrent.SettableFuture;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.impl.internal.ui.AbstractVcsLogUi;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogCommitModel;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VcsLogImpl implements VcsLog {
  
  private final VcsLogDataImpl myLogData;
  
  private final AbstractVcsLogUi myUi;

  public VcsLogImpl(VcsLogDataImpl manager, AbstractVcsLogUi ui) {
    myLogData = manager;
    myUi = ui;
  }

  @Override
  
  public List<CommitId> getSelectedCommits() {
    return getSelectedDataFromTable(VcsLogCommitModel::getCommitIdAtRow);
  }

  
  @Override
  public List<VcsShortCommitDetails> getSelectedShortDetails() {
    return getSelectedDataFromTable(VcsLogCommitModel::getShortDetails);
  }

  
  @Override
  public List<VcsFullCommitDetails> getSelectedDetails() {
    return getSelectedDataFromTable(VcsLogCommitModel::getFullDetails);
  }

  @Override
  public void requestSelectedDetails(Consumer<List<VcsFullCommitDetails>> consumer, @Nullable ProgressIndicator indicator) {
    List<Integer> rowsList = myUi.getSelectedRows();
    myLogData.getCommitDetailsGetter()
      .loadCommitsData(myUi.getCommitModel().convertToCommitIds(rowsList), consumer, indicator);
  }

  @Override
  public @Nullable Collection<String> getContainingBranches(Hash commitHash, VirtualFile root) {
    return myLogData.getContainingBranchesGetter().getContainingBranchesFromCache(root, commitHash);
  }

  
  @Override
  public Future<Boolean> jumpToReference(String reference) {
    SettableFuture<Boolean> future = SettableFuture.create();
    VcsLogRefs refs = myUi.getDataPack().getRefs();
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      List<VcsRef> matchingRefs = refs.stream().filter(ref -> ref.getName().startsWith(reference)).collect(Collectors.toList());
      ApplicationManager.getApplication().invokeLater(() -> {
        if (matchingRefs.isEmpty()) {
          myUi.jumpToCommitByPartOfHash(reference, future);
        }
        else {
          VcsRef ref = Collections.min(matchingRefs, new VcsGoToRefComparator(myUi.getDataPack().getLogProviders()));
          myUi.jumpToCommit(ref.getCommitHash(), ref.getRoot(), future);
        }
      });
    });
    return future;
  }

  
  @Override
  public Map<VirtualFile, VcsLogProvider> getLogProviders() {
    return myLogData.getLogProviders();
  }

  private <T> List<T> getSelectedDataFromTable(BiFunction<VcsLogCommitModel, Integer, T> dataGetter) {
    List<Integer> rows = myUi.getSelectedRows();
    VcsLogCommitModel model = myUi.getCommitModel();
    return new AbstractList<T>() {
      @Override
      public T get(int index) {
        return dataGetter.apply(model, rows.get(index));
      }

      @Override
      public int size() {
        return rows.size();
      }
    };
  }
}
