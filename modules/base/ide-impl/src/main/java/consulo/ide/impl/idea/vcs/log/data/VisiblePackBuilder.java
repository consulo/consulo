/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.data;

import consulo.application.util.function.Computable;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.log.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.ide.impl.idea.vcs.log.data.index.VcsLogIndex;
import consulo.versionControlSystem.log.graph.GraphCommit;
import consulo.ide.impl.idea.vcs.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.graph.VisibleGraph;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogFilterCollectionImpl.VcsLogFilterCollectionBuilder;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogHashFilterImpl;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.versionControlSystem.util.StopWatch;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

class VisiblePackBuilder {

  private static final Logger LOG = Logger.getInstance(VisiblePackBuilder.class);

  @Nonnull
  private final VcsLogStorage myHashMap;
  @Nonnull
  private final TopCommitsCache myTopCommitsDetailsCache;
  @Nonnull
  private final DataGetter<VcsFullCommitDetails> myCommitDetailsGetter;
  @Nonnull
  private final Map<VirtualFile, VcsLogProvider> myLogProviders;
  @Nonnull
  private final VcsLogIndex myIndex;

  VisiblePackBuilder(@Nonnull Map<VirtualFile, VcsLogProvider> providers,
                     @Nonnull VcsLogStorage hashMap,
                     @Nonnull TopCommitsCache topCommitsDetailsCache,
                     @Nonnull DataGetter<VcsFullCommitDetails> detailsGetter,
                     @Nonnull VcsLogIndex index) {
    myHashMap = hashMap;
    myTopCommitsDetailsCache = topCommitsDetailsCache;
    myCommitDetailsGetter = detailsGetter;
    myLogProviders = providers;
    myIndex = index;
  }

  @Nonnull
  Pair<VisiblePack, CommitCountStage> build(@Nonnull DataPack dataPack,
                                            @Nonnull PermanentGraph.SortType sortType,
                                            @Nonnull VcsLogFilterCollection filters,
                                            @Nonnull CommitCountStage commitCount) {
    long start = System.currentTimeMillis();

    VcsLogHashFilter hashFilter = filters.getHashFilter();
    if (hashFilter != null && !hashFilter.getHashes().isEmpty()) { // hashes should be shown, no matter if they match other filters or not
      return Pair.create(applyHashFilter(dataPack, hashFilter.getHashes(), sortType), commitCount);
    }

    Collection<VirtualFile> visibleRoots =
            VcsLogUtil.getAllVisibleRoots(dataPack.getLogProviders().keySet(), filters.getRootFilter(), filters.getStructureFilter());
    Set<Integer> matchingHeads = getMatchingHeads(dataPack.getRefsModel(), visibleRoots, filters);
    FilterResult filterResult = filterByDetails(dataPack, filters, commitCount, visibleRoots, matchingHeads);

    VisibleGraph<Integer> visibleGraph;
    if (matchesNothing(matchingHeads) || matchesNothing(filterResult.matchingCommits)) {
      visibleGraph = EmptyVisibleGraph.getInstance();
    }
    else {
      visibleGraph = dataPack.getPermanentGraph().createVisibleGraph(sortType, matchingHeads, filterResult.matchingCommits);
    }

    LOG.debug(StopWatch.formatTime(System.currentTimeMillis() - start) + " for filtering by " + filters);

    return Pair.create(new VisiblePack(dataPack, visibleGraph, filterResult.canRequestMore, filters), filterResult.commitCount);
  }

  @Nonnull
  private FilterResult filterByDetails(
    @Nonnull DataPack dataPack,
    @Nonnull VcsLogFilterCollection filters,
    @Nonnull CommitCountStage commitCount,
    @Nonnull Collection<VirtualFile> visibleRoots,
    @Nullable Set<Integer> matchingHeads
  ) {
    List<VcsLogDetailsFilter> detailsFilters = filters.getDetailsFilters();
    if (detailsFilters.isEmpty()) return new FilterResult(null, false, commitCount);

    Set<Integer> filteredWidthIndex = null;
    if (myIndex.canFilter(detailsFilters)) {
      Collection<VirtualFile> notIndexedRoots = ContainerUtil.filter(visibleRoots, root -> !myIndex.isIndexed(root));

      if (notIndexedRoots.size() < visibleRoots.size()) {
        filteredWidthIndex = myIndex.filter(detailsFilters);
        if (notIndexedRoots.isEmpty()) return new FilterResult(filteredWidthIndex, false, commitCount);
        matchingHeads = getMatchingHeads(dataPack.getRefsModel(), notIndexedRoots, filters);
      }
    }

    FilterResult filteredWithVcs = filterWithVcs(dataPack.getPermanentGraph(), filters, detailsFilters, matchingHeads, commitCount);

    Set<Integer> filteredCommits;
    if (filteredWidthIndex == null) {
      filteredCommits = filteredWithVcs.matchingCommits;
    }
    else if (filteredWithVcs.matchingCommits == null) {
      filteredCommits = filteredWidthIndex;
    }
    else {
      filteredCommits = ContainerUtil.union(filteredWidthIndex, filteredWithVcs.matchingCommits);
    }
    return new FilterResult(filteredCommits, filteredWithVcs.canRequestMore, filteredWithVcs.commitCount);
  }

  @Nonnull
  private FilterResult filterWithVcs(
    @Nonnull PermanentGraph graph,
    @Nonnull VcsLogFilterCollection filters,
    @Nonnull List<VcsLogDetailsFilter> detailsFilters,
    @Nullable Set<Integer> matchingHeads,
    @Nonnull CommitCountStage commitCount
  ) {
    Set<Integer> matchingCommits = null;
    if (commitCount == CommitCountStage.INITIAL) {
      matchingCommits = getMatchedCommitIndex(filterInMemory(graph, detailsFilters, matchingHeads));
      if (matchingCommits.size() < commitCount.getCount()) {
        commitCount = commitCount.next();
        matchingCommits = null;
      }
    }

    if (matchingCommits == null) {
      try {
        matchingCommits = getMatchedCommitIndex(getFilteredDetailsFromTheVcs(myLogProviders, filters, commitCount.getCount()));
      }
      catch (VcsException e) {
        //TODO show an error balloon or something else for non-ea guys.
        matchingCommits = Collections.emptySet();
        LOG.error(e);
      }
    }

    return new FilterResult(matchingCommits, matchingCommits.size() >= commitCount.getCount(), commitCount);
  }

  private static <T> boolean matchesNothing(@Nullable Collection<T> matchingSet) {
    return matchingSet != null && matchingSet.isEmpty();
  }

  private VisiblePack applyHashFilter(
    @Nonnull DataPack dataPack,
    @Nonnull Collection<String> hashes,
    @Nonnull PermanentGraph.SortType sortType
  ) {
    final Set<Integer> indices = ContainerUtil.map2SetNotNull(hashes, partOfHash -> {
      CommitId commitId = myHashMap.findCommitId(new CommitIdByStringCondition(partOfHash));
      return commitId != null ? myHashMap.getCommitIndex(commitId.getHash(), commitId.getRoot()) : null;
    });
    VisibleGraph<Integer> visibleGraph = dataPack.getPermanentGraph().createVisibleGraph(sortType, null, indices);
    return new VisiblePack(
      dataPack,
      visibleGraph,
      false,
      new VcsLogFilterCollectionBuilder().with(new VcsLogHashFilterImpl(hashes)).build()
    );
  }

  @Nullable
  private Set<Integer> getMatchingHeads(
    @Nonnull VcsLogRefs refs,
    @Nonnull Collection<VirtualFile> roots,
    @Nonnull VcsLogFilterCollection filters
  ) {
    VcsLogBranchFilter branchFilter = filters.getBranchFilter();
    VcsLogRootFilter rootFilter = filters.getRootFilter();
    VcsLogStructureFilter structureFilter = filters.getStructureFilter();

    if (branchFilter == null && rootFilter == null && structureFilter == null) return null;

    Set<Integer> filteredByBranch = null;

    if (branchFilter != null) {
      filteredByBranch = getMatchingHeads(refs, branchFilter);
    }

    Set<Integer> filteredByFile = getMatchingHeads(refs, roots);

    if (filteredByBranch == null) return filteredByFile;
    if (filteredByFile == null) return filteredByBranch;

    return new HashSet<>(ContainerUtil.intersection(filteredByBranch, filteredByFile));
  }

  private Set<Integer> getMatchingHeads(@Nonnull VcsLogRefs refs, @Nonnull final VcsLogBranchFilter filter) {
    return new HashSet<>(ContainerUtil.mapNotNull(refs.getBranches(), ref -> {
      boolean acceptRef = filter.matches(ref.getName());
      return acceptRef ? myHashMap.getCommitIndex(ref.getCommitHash(), ref.getRoot()) : null;
    }));
  }

  private Set<Integer> getMatchingHeads(@Nonnull VcsLogRefs refs, @Nonnull Collection<VirtualFile> roots) {
    Set<Integer> result = new HashSet<>();
    for (VcsRef branch : refs.getBranches()) {
      if (roots.contains(branch.getRoot())) {
        result.add(myHashMap.getCommitIndex(branch.getCommitHash(), branch.getRoot()));
      }
    }
    return result;
  }

  @Nonnull
  private Collection<CommitId> filterInMemory(
    @Nonnull PermanentGraph<Integer> permanentGraph,
    @Nonnull List<VcsLogDetailsFilter> detailsFilters,
    @Nullable Set<Integer> matchingHeads
  ) {
    Collection<CommitId> result = new ArrayList<>();
    for (GraphCommit<Integer> commit : permanentGraph.getAllCommits()) {
      VcsCommitMetadata data = getDetailsFromCache(commit.getId());
      if (data == null) {
        // no more continuous details in the cache
        break;
      }
      if (matchesAllFilters(data, permanentGraph, detailsFilters, matchingHeads)) {
        result.add(new CommitId(data.getId(), data.getRoot()));
      }
    }
    return result;
  }

  private boolean matchesAllFilters(
    @Nonnull final VcsCommitMetadata commit,
    @Nonnull final PermanentGraph<Integer> permanentGraph,
    @Nonnull List<VcsLogDetailsFilter> detailsFilters,
    @Nullable final Set<Integer> matchingHeads
  ) {
    boolean matchesAllDetails = ContainerUtil.and(detailsFilters, filter -> filter.matches(commit));
    return matchesAllDetails && matchesAnyHead(permanentGraph, commit, matchingHeads);
  }

  private boolean matchesAnyHead(
    @Nonnull PermanentGraph<Integer> permanentGraph,
    @Nonnull VcsCommitMetadata commit,
    @Nullable Set<Integer> matchingHeads
  ) {
    if (matchingHeads == null) {
      return true;
    }
    // TODO O(n^2)
    int commitIndex = myHashMap.getCommitIndex(commit.getId(), commit.getRoot());
    return ContainerUtil.intersects(permanentGraph.getContainingBranches(commitIndex), matchingHeads);
  }

  @Nullable
  private VcsCommitMetadata getDetailsFromCache(final int commitIndex) {
    VcsCommitMetadata details = myTopCommitsDetailsCache.get(commitIndex);
    if (details != null) {
      return details;
    }
    return UIUtil.invokeAndWaitIfNeeded((Computable<VcsCommitMetadata>)() -> myCommitDetailsGetter.getCommitDataIfAvailable(commitIndex));
  }

  @Nonnull
  private static Collection<CommitId> getFilteredDetailsFromTheVcs(
    @Nonnull Map<VirtualFile, VcsLogProvider> providers,
    @Nonnull VcsLogFilterCollection filterCollection,
    int maxCount
  ) throws VcsException {
    Set<VirtualFile> visibleRoots =
      VcsLogUtil.getAllVisibleRoots(providers.keySet(), filterCollection.getRootFilter(), filterCollection.getStructureFilter());

    Collection<CommitId> commits = new ArrayList<>();
    for (Map.Entry<VirtualFile, VcsLogProvider> entry : providers.entrySet()) {
      final VirtualFile root = entry.getKey();

      if (!visibleRoots.contains(root) ||
          (filterCollection.getUserFilter() != null && filterCollection.getUserFilter().getUsers(root).isEmpty())) {
        // there is a structure or user filter, but it doesn't match this root
        continue;
      }

      VcsLogFilterCollection rootSpecificCollection = filterCollection;
      if (rootSpecificCollection.getStructureFilter() != null) {
        rootSpecificCollection = new VcsLogFilterCollectionBuilder(filterCollection)
          .with(new VcsLogStructureFilterImpl(ContainerUtil.newHashSet(VcsLogUtil.getFilteredFilesForRoot(root, filterCollection))))
          .build();
      }

      List<TimedVcsCommit> matchingCommits = entry.getValue().getCommitsMatchingFilter(root, rootSpecificCollection, maxCount);
      commits.addAll(ContainerUtil.map(matchingCommits, commit -> new CommitId(commit.getId(), root)));
    }

    return commits;
  }

  @Nullable
  private Set<Integer> getMatchedCommitIndex(@Nullable Collection<CommitId> commits) {
    if (commits == null) {
      return null;
    }

    return ContainerUtil.map2Set(commits, commitId -> myHashMap.getCommitIndex(commitId.getHash(), commitId.getRoot()));
  }

  private static class FilterResult {
    @Nullable private final Set<Integer> matchingCommits;
    private final boolean canRequestMore;
    @Nonnull
    private final CommitCountStage commitCount;

    private FilterResult(@Nullable Set<Integer> commits, boolean more, @Nonnull CommitCountStage count) {
      matchingCommits = commits;
      canRequestMore = more;
      commitCount = count;
    }
  }
}
