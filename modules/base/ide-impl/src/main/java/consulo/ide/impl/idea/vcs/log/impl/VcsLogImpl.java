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
package consulo.ide.impl.idea.vcs.log.impl;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.SettableFuture;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.versionControlSystem.log.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.vcs.log.data.VcsLogDataImpl;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.ide.impl.idea.vcs.log.ui.frame.VcsLogGraphTable;
import consulo.ide.impl.idea.vcs.log.ui.tables.GraphTableModel;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VcsLogImpl implements VcsLog {
  @Nonnull
  private final VcsLogDataImpl myLogData;
  @Nonnull
  private final VcsLogUiImpl myUi;

  public VcsLogImpl(@Nonnull VcsLogDataImpl manager, @Nonnull VcsLogUiImpl ui) {
    myLogData = manager;
    myUi = ui;
  }

  @Override
  @Nonnull
  public List<CommitId> getSelectedCommits() {
    return getSelectedDataFromTable(GraphTableModel::getCommitIdAtRow);
  }

  @Nonnull
  @Override
  public List<VcsShortCommitDetails> getSelectedShortDetails() {
    return getSelectedDataFromTable(GraphTableModel::getShortDetails);
  }

  @Nonnull
  @Override
  public List<VcsFullCommitDetails> getSelectedDetails() {
    return getSelectedDataFromTable(GraphTableModel::getFullDetails);
  }

  @Override
  public void requestSelectedDetails(@Nonnull Consumer<List<VcsFullCommitDetails>> consumer, @Nullable ProgressIndicator indicator) {
    List<Integer> rowsList = Ints.asList(myUi.getTable().getSelectedRows());
    myLogData.getCommitDetailsGetter()
      .loadCommitsData(getTable().getModel().convertToCommitIds(rowsList), consumer, indicator);
  }

  @Nullable
  @Override
  public Collection<String> getContainingBranches(@Nonnull Hash commitHash, @Nonnull VirtualFile root) {
    return myLogData.getContainingBranchesGetter().getContainingBranchesFromCache(root, commitHash);
  }

  @Nonnull
  @Override
  public Future<Boolean> jumpToReference(final String reference) {
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

  @Nonnull
  @Override
  public Map<VirtualFile, VcsLogProvider> getLogProviders() {
    return myLogData.getLogProviders();
  }

  @Nonnull
  private VcsLogGraphTable getTable() {
    return myUi.getTable();
  }

  @Nonnull
  private <T> List<T> getSelectedDataFromTable(@Nonnull BiFunction<GraphTableModel, Integer, T> dataGetter) {
    final int[] rows = myUi.getTable().getSelectedRows();
    return new AbstractList<T>() {
      @Nonnull
      @Override
      public T get(int index) {
        return dataGetter.apply(getTable().getModel(), rows[index]);
      }

      @Override
      public int size() {
        return rows.length;
      }
    };
  }
}
