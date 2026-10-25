// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.log;

import consulo.util.dataholder.Key;

import java.util.List;
import java.util.function.Consumer;

/**
 * Selection of commits in the Vcs Log Graph.
 *
 * @see #KEY
 */
public interface VcsLogCommitSelection {
    Key<VcsLogCommitSelection> KEY = Key.create(VcsLogCommitSelection.class);

    /**
     * Rows in the visible graph.
     */
    int[] getRows();

    /**
     * Identifiers of the selected commits in the visible graph.
     *
     * @see VcsLogDataProvider#getCommitIndex
     */
    List<Integer> getIds();

    /**
     * {@link CommitId} of the commits selected in the table.
     */
    List<CommitId> getCommits();

    /**
     * Cached metadata of the commits selected in the table.
     * When metadata of the commit is not available in the cache, a placeholder object is returned.
     * <p>
     * Metadata are loaded faster than full details and since it is done while scrolling,
     * there is a better chance that details for a commit are loaded when user selects it.
     */
    List<VcsShortCommitDetails> getCachedMetadata();

    /**
     * Cached full details of the commits selected in the table.
     * When full details of the commit are not available in the cache, a placeholder object is returned.
     */
    List<VcsFullCommitDetails> getCachedFullDetails();

    /**
     * Sends a request to load full details of the selected commits in a background thread.
     * After all details are loaded they are provided to the consumer in the EDT.
     *
     * @param consumer called in EDT after all details are loaded.
     */
    void requestFullDetails(Consumer<List<VcsFullCommitDetails>> consumer);
}
