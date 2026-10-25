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

import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsLog;
import consulo.versionControlSystem.log.VcsLogCommitSelection;
import consulo.versionControlSystem.log.VcsShortCommitDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public class VcsLogCommitSelectionImpl implements VcsLogCommitSelection {
    private final VcsLog myLog;
    private final List<Integer> myRows;
    private final VcsLogCommitModel myModel;

    public VcsLogCommitSelectionImpl(VcsLog log, List<Integer> rows, VcsLogCommitModel model) {
        myLog = log;
        myRows = List.copyOf(rows);
        myModel = model;
    }

    @Override
    public int[] getRows() {
        return myRows.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public List<Integer> getIds() {
        List<Integer> ids = new ArrayList<>(myRows.size());
        for (int row : myRows) {
            ids.add(myModel.getIdAtRow(row));
        }
        return ids;
    }

    @Override
    public List<CommitId> getCommits() {
        List<CommitId> commits = new ArrayList<>(myRows.size());
        for (int row : myRows) {
            CommitId commitId = myModel.getCommitIdAtRow(row);
            if (commitId != null) {
                commits.add(commitId);
            }
        }
        return commits;
    }

    @Override
    public List<VcsShortCommitDetails> getCachedMetadata() {
        return myLog.getSelectedShortDetails();
    }

    @Override
    public List<VcsFullCommitDetails> getCachedFullDetails() {
        return myLog.getSelectedDetails();
    }

    @Override
    public void requestFullDetails(Consumer<List<VcsFullCommitDetails>> consumer) {
        myLog.requestSelectedDetails(consumer, null);
    }
}
