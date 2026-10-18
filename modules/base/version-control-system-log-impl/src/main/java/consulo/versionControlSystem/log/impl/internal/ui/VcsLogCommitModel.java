/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.VcsShortCommitDetails;
import consulo.versionControlSystem.log.impl.internal.data.CommitIdByStringCondition;
import consulo.versionControlSystem.log.impl.internal.data.DataGetter;
import consulo.versionControlSystem.log.impl.internal.data.RefsModel;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogFilterer;
import consulo.versionControlSystem.log.impl.internal.data.VisiblePack;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class VcsLogCommitModel {
    private static final int UP_PRELOAD_COUNT = 20;
    private static final int DOWN_PRELOAD_COUNT = 40;

    private final VcsLogDataImpl myLogData;
    private final VcsLogFilterer myFilterer;
    private final Runnable myLoadMoreStarted;

    private VisiblePack myDataPack;
    private boolean myMoreRequested;

    public VcsLogCommitModel(VisiblePack dataPack, VcsLogDataImpl logData, VcsLogFilterer filterer, Runnable loadMoreStarted) {
        myLogData = logData;
        myFilterer = filterer;
        myLoadMoreStarted = loadMoreStarted;
        myDataPack = dataPack;
    }

    public int getRowCount() {
        return myDataPack.getVisibleGraph().getVisibleCommitCount();
    }

    public VirtualFile getRoot(int row) {
        return myDataPack.getRoot(row);
    }

    public Integer getIdAtRow(int row) {
        return myDataPack.getVisibleGraph().getRowInfo(row).getCommit();
    }

    public @Nullable CommitId getCommitIdAtRow(int row) {
        return myLogData.getCommitId(getIdAtRow(row));
    }

    public int getRowOfCommit(Hash hash, VirtualFile root) {
        int commitIndex = myLogData.getCommitIndex(hash, root);
        return getRowOfCommitIndex(commitIndex);
    }

    public int getRowOfCommitIndex(int commitIndex) {
        return ContainerUtil.indexOf(VcsLogUtil.getVisibleCommits(myDataPack.getVisibleGraph()), i -> i == commitIndex);
    }

    public int getRowOfCommitByPartOfHash(String partialHash) {
        CommitIdByStringCondition hashByString = new CommitIdByStringCondition(partialHash);
        CommitId commitId = myLogData.getHashMap().findCommitId(
            candidate -> hashByString.test(candidate) && getRowOfCommit(candidate.getHash(), candidate.getRoot()) != -1
        );
        return commitId != null ? getRowOfCommit(commitId.getHash(), commitId.getRoot()) : -1;
    }

    /**
     * Requests the proper data provider to load more data from the log & recreate the model.
     *
     * @param onLoaded will be called upon task completion on the UI thread.
     */
    public void requestToLoadMore(Runnable onLoaded) {
        myMoreRequested = true;
        myFilterer.moreCommitsNeeded(onLoaded);
        myLoadMoreStarted.run();
    }

    /**
     * Returns true if not all data has been loaded, i.e. there is sense to {@link #requestToLoadMore(Runnable) request more data}.
     */
    public boolean canRequestMore() {
        return !myMoreRequested && myDataPack.canRequestMore();
    }

    public void setVisiblePack(VisiblePack visiblePack) {
        myDataPack = visiblePack;
        myMoreRequested = false;
    }

    public VisiblePack getVisiblePack() {
        return myDataPack;
    }

    public VcsFullCommitDetails getFullDetails(int row) {
        return getDetails(row, myLogData.getCommitDetailsGetter());
    }

    public VcsShortCommitDetails getShortDetails(int row) {
        return getDetails(row, myLogData.getMiniDetailsGetter());
    }

    private <T extends VcsShortCommitDetails> T getDetails(int row, DataGetter<T> dataGetter) {
        Iterable<Integer> iterable = createRowsIterable(row, UP_PRELOAD_COUNT, DOWN_PRELOAD_COUNT, getRowCount());
        return dataGetter.getCommitData(getIdAtRow(row), iterable);
    }

    public Collection<VcsRef> getRefsAtRow(int row) {
        return ((RefsModel) myDataPack.getRefs()).refsToCommit(getIdAtRow(row));
    }

    public List<VcsRef> getBranchesAtRow(int row) {
        return getRefsAtRow(row).stream().filter(ref -> ref.getType().isBranch()).collect(Collectors.toList());
    }

    public List<Integer> convertToCommitIds(List<Integer> rows) {
        return ContainerUtil.map(rows, this::getIdAtRow);
    }

    private Iterable<Integer> createRowsIterable(int row, int above, int below, int maxRows) {
        return () -> new Iterator<>() {
            private int myRowIndex = Math.max(0, row - above);

            @Override
            public boolean hasNext() {
                return myRowIndex < row + below && myRowIndex < maxRows;
            }

            @Override
            public Integer next() {
                int nextRow = myRowIndex;
                myRowIndex++;
                return getIdAtRow(nextRow);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Removing elements is not supported.");
            }
        };
    }
}
