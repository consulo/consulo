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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.graph.GraphCommit;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.graph.PermanentGraphInfo;
import consulo.versionControlSystem.log.graph.VisibleGraph;
import consulo.versionControlSystem.log.impl.internal.VcsLogFilterCollectionImpl;
import consulo.versionControlSystem.log.impl.internal.VcsLogHashFilterImpl;
import consulo.versionControlSystem.log.impl.internal.data.index.VcsLogIndex;
import consulo.versionControlSystem.log.impl.internal.graph.ReachableNodes;
import consulo.versionControlSystem.log.impl.internal.util.UnsignedBitSet;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.versionControlSystem.util.StopWatch;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.*;

class VisiblePackBuilder {
    private static final Logger LOG = Logger.getInstance(VisiblePackBuilder.class);

    
    private final VcsLogStorage myHashMap;
    
    private final TopCommitsCache myTopCommitsDetailsCache;
    
    private final DataGetter<VcsFullCommitDetails> myCommitDetailsGetter;
    
    private final Map<VirtualFile, VcsLogProvider> myLogProviders;
    
    private final VcsLogIndex myIndex;

    VisiblePackBuilder(
        Map<VirtualFile, VcsLogProvider> providers,
        VcsLogStorage hashMap,
        TopCommitsCache topCommitsDetailsCache,
        DataGetter<VcsFullCommitDetails> detailsGetter,
        VcsLogIndex index
    ) {
        myHashMap = hashMap;
        myTopCommitsDetailsCache = topCommitsDetailsCache;
        myCommitDetailsGetter = detailsGetter;
        myLogProviders = providers;
        myIndex = index;
    }

    
    Pair<VisiblePack, CommitCountStage> build(
        DataPack dataPack,
        PermanentGraph.SortType sortType,
        VcsLogFilterCollection filters,
        CommitCountStage commitCount
    ) {
        long start = System.currentTimeMillis();

        VcsLogHashFilter hashFilter = filters.getHashFilter();
        if (hashFilter != null && !hashFilter.getHashes().isEmpty()) {
            // hashes should be shown, no matter if they match other filters or not
            return Pair.create(applyHashFilter(dataPack, hashFilter.getHashes(), sortType), commitCount);
        }

        Collection<VirtualFile> visibleRoots =
            VcsLogUtil.getAllVisibleRoots(dataPack.getLogProviders().keySet(), filters.getRootFilter(), filters.getStructureFilter());
        Set<Integer> matchingHeads;
        Set<Integer> commitCandidates = null;
        boolean forceFilterByVcs = false;

        VcsLogRangeFilter rangeFilter = filters.getRangeFilter();
        if (rangeFilter != null) {
            Pair<@Nullable Set<Integer>, Set<Integer>> commitsAndHeads = filterByRange(dataPack, filters, visibleRoots, rangeFilter);

            commitCandidates = commitsAndHeads.first;
            matchingHeads = commitsAndHeads.second;
            forceFilterByVcs = commitCandidates == null;
        }
        else {
            matchingHeads = getMatchingHeads(dataPack.getRefsModel(), visibleRoots, filters);
        }

        FilterResult filterResult =
            filterByDetails(dataPack, filters, commitCount, visibleRoots, matchingHeads, commitCandidates, forceFilterByVcs);

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

    
    private FilterResult filterByDetails(
        DataPack dataPack,
        VcsLogFilterCollection filters,
        CommitCountStage commitCount,
        Collection<VirtualFile> visibleRoots,
        @Nullable Set<Integer> matchingHeads,
        @Nullable Set<Integer> commitCandidates,
        boolean forceFilterByVcs
    ) {
        List<VcsLogDetailsFilter> detailsFilters = filters.getDetailsFilters();
        if (!forceFilterByVcs && detailsFilters.isEmpty()) {
            return new FilterResult(commitCandidates, false, commitCount);
        }

        Set<Integer> filteredWidthIndex = null;
        if (!forceFilterByVcs && myIndex.canFilter(detailsFilters)) {
            Collection<VirtualFile> notIndexedRoots = ContainerUtil.filter(visibleRoots, root -> !myIndex.isIndexed(root));

            if (notIndexedRoots.size() < visibleRoots.size()) {
                filteredWidthIndex = intersect(myIndex.filter(detailsFilters), commitCandidates);
                if (notIndexedRoots.isEmpty()) {
                    return new FilterResult(filteredWidthIndex, false, commitCount);
                }
                matchingHeads = getMatchingHeads(dataPack.getRefsModel(), notIndexedRoots, filters);
            }
        }

        FilterResult filteredWithVcs =
            filterWithVcs(dataPack.getPermanentGraph(), filters, detailsFilters, matchingHeads, commitCount, commitCandidates);

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

    
    private FilterResult filterWithVcs(
        PermanentGraph graph,
        VcsLogFilterCollection filters,
        List<VcsLogDetailsFilter> detailsFilters,
        @Nullable Set<Integer> matchingHeads,
        CommitCountStage commitCount,
        @Nullable Set<Integer> commitCandidates
    ) {
        Set<Integer> matchingCommits = null;
        if (commitCount == CommitCountStage.INITIAL) {
            if (filters.getRangeFilter() == null) { // not filtering in memory by range for simplicity
                matchingCommits = getMatchedCommitIndex(filterInMemory(graph, detailsFilters, matchingHeads, commitCandidates));
                if (matchingCommits.size() < commitCount.getCount()) {
                    commitCount = commitCount.next();
                    matchingCommits = null;
                }
            }
            else {
                commitCount = commitCount.next();
            }
        }

        if (matchingCommits == null) {
            try {
                matchingCommits = intersect(
                    getMatchedCommitIndex(getFilteredDetailsFromTheVcs(myLogProviders, filters, commitCount.getCount())),
                    commitCandidates
                );
            }
            catch (VcsException e) {
                //TODO show an error balloon or something else for non-ea guys.
                matchingCommits = Collections.emptySet();
                LOG.error(e);
            }
        }

        return new FilterResult(matchingCommits, matchingCommits.size() >= commitCount.getCount(), commitCount);
    }

    private Pair<@Nullable Set<Integer>, Set<Integer>> filterByRange(
        DataPack dataPack,
        VcsLogFilterCollection filters,
        Collection<VirtualFile> visibleRoots,
        VcsLogRangeFilter rangeFilter
    ) {
        /*
          If we have both a range filter and a branch filter (e.g. `183\nmaster..feature`) they should be united: the graph should show both
          commits contained in the range, and commits reachable from branches.

          But the main filtering logic is opposite: matchingHeads + some other filter => makes the intersection of commits.
          To overcome this logic for the range filter case, we are not using matchingHeads, but are collecting all commits reachable from
          matchingHeads, and unite them with commits belonging to the range.
        */
        VcsLogBranchFilter branchFilter = filters.getBranchFilter();
        Set<Integer> commitsReachableFromHeads = new HashSet<>();
        if (branchFilter != null) {
            Set<Integer> explicitMatchingHeads = getMatchingHeads(dataPack.getRefsModel(), branchFilter);
            commitsReachableFromHeads = collectCommitsReachableFromHeads(dataPack, explicitMatchingHeads);
        }

        RangeFilterResult commitsForRangeFilter = VcsLogRangeFilterUtil.filterByRange(myHashMap, myLogProviders, dataPack, rangeFilter);
        Set<Integer> commits = null;
        if (commitsForRangeFilter instanceof RangeFilterResult.Commits rangeCommits) {
            commits = new HashSet<>(commitsReachableFromHeads);
            commits.addAll(rangeCommits.commits());
        }

        /*
          At the same time, the root filter should intersect with the range filter (and the branch filter),
          therefore we take matching heads from the root filter, but use reachable commits set for the branch filter.
        */
        Set<Integer> heads = getMatchingHeads(dataPack.getRefsModel(), visibleRoots);
        return Pair.create(commits, heads);
    }

    @SuppressWarnings("unchecked")
    private static Set<Integer> collectCommitsReachableFromHeads(DataPack dataPack, Set<Integer> matchingHeads) {
        Set<Integer> result = new HashSet<>();
        if (!(dataPack.getPermanentGraph() instanceof PermanentGraphInfo<?> info)) {
            return result;
        }
        PermanentGraphInfo<Integer> permanentGraphInfo = (PermanentGraphInfo<Integer>) info;
        Set<Integer> headNodes = permanentGraphInfo.getPermanentCommitsInfo().convertToNodeIds(matchingHeads);
        UnsignedBitSet reachable = ReachableNodes.getReachableNodes(permanentGraphInfo.getLinearGraph(), headNodes);
        for (int node = 0; node < permanentGraphInfo.getLinearGraph().nodesCount(); node++) {
            if (reachable.get(node)) {
                result.add(permanentGraphInfo.getPermanentCommitsInfo().getCommitId(node));
            }
        }
        return result;
    }

    private static @Nullable Set<Integer> intersect(@Nullable Set<Integer> commits, @Nullable Set<Integer> candidates) {
        if (commits == null || candidates == null) {
            return commits == null ? candidates : commits;
        }
        Set<Integer> result = new HashSet<>(commits);
        result.retainAll(candidates);
        return result;
    }

    private static <T> boolean matchesNothing(@Nullable Collection<T> matchingSet) {
        return matchingSet != null && matchingSet.isEmpty();
    }

    private VisiblePack applyHashFilter(
        DataPack dataPack,
        Collection<String> hashes,
        PermanentGraph.SortType sortType
    ) {
        Set<Integer> indices = ContainerUtil.map2SetNotNull(
            hashes,
            partOfHash -> {
                CommitId commitId = myHashMap.findCommitId(new CommitIdByStringCondition(partOfHash));
                return commitId != null ? myHashMap.getCommitIndex(commitId.getHash(), commitId.getRoot()) : null;
            }
        );
        VisibleGraph<Integer> visibleGraph = dataPack.getPermanentGraph().createVisibleGraph(sortType, null, indices);
        return new VisiblePack(
            dataPack,
            visibleGraph,
            false,
            new VcsLogFilterCollectionImpl.VcsLogFilterCollectionBuilder().with(new VcsLogHashFilterImpl(hashes)).build()
        );
    }

    private @Nullable Set<Integer> getMatchingHeads(
        VcsLogRefs refs,
        Collection<VirtualFile> roots,
        VcsLogFilterCollection filters
    ) {
        VcsLogBranchFilter branchFilter = filters.getBranchFilter();
        VcsLogRootFilter rootFilter = filters.getRootFilter();
        VcsLogStructureFilter structureFilter = filters.getStructureFilter();

        if (branchFilter == null && rootFilter == null && structureFilter == null) {
            return null;
        }

        Set<Integer> filteredByBranch = null;

        if (branchFilter != null) {
            filteredByBranch = getMatchingHeads(refs, branchFilter);
        }

        Set<Integer> filteredByFile = getMatchingHeads(refs, roots);

        if (filteredByBranch == null) {
            return filteredByFile;
        }
        if (filteredByFile == null) {
            return filteredByBranch;
        }

        return new HashSet<>(ContainerUtil.intersection(filteredByBranch, filteredByFile));
    }

    private Set<Integer> getMatchingHeads(VcsLogRefs refs, VcsLogBranchFilter filter) {
        return new HashSet<>(ContainerUtil.mapNotNull(refs.getBranches(), ref -> {
            boolean acceptRef = filter.matches(ref.getName());
            return acceptRef ? myHashMap.getCommitIndex(ref.getCommitHash(), ref.getRoot()) : null;
        }));
    }

    private Set<Integer> getMatchingHeads(VcsLogRefs refs, Collection<VirtualFile> roots) {
        Set<Integer> result = new HashSet<>();
        for (VcsRef branch : refs.getBranches()) {
            if (roots.contains(branch.getRoot())) {
                result.add(myHashMap.getCommitIndex(branch.getCommitHash(), branch.getRoot()));
            }
        }
        return result;
    }

    
    private Collection<CommitId> filterInMemory(
        PermanentGraph<Integer> permanentGraph,
        List<VcsLogDetailsFilter> detailsFilters,
        @Nullable Set<Integer> matchingHeads,
        @Nullable Set<Integer> commitCandidates
    ) {
        Collection<CommitId> result = new ArrayList<>();
        for (GraphCommit<Integer> commit : permanentGraph.getAllCommits()) {
            if (commitCandidates != null && !commitCandidates.contains(commit.getId())) {
                continue;
            }
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
        VcsCommitMetadata commit,
        PermanentGraph<Integer> permanentGraph,
        List<VcsLogDetailsFilter> detailsFilters,
        @Nullable Set<Integer> matchingHeads
    ) {
        boolean matchesAllDetails = ContainerUtil.and(detailsFilters, filter -> filter.matches(commit));
        return matchesAllDetails && matchesAnyHead(permanentGraph, commit, matchingHeads);
    }

    private boolean matchesAnyHead(
        PermanentGraph<Integer> permanentGraph,
        VcsCommitMetadata commit,
        @Nullable Set<Integer> matchingHeads
    ) {
        if (matchingHeads == null) {
            return true;
        }
        // TODO O(n^2)
        int commitIndex = myHashMap.getCommitIndex(commit.getId(), commit.getRoot());
        return ContainerUtil.intersects(permanentGraph.getContainingBranches(commitIndex), matchingHeads);
    }

    private @Nullable VcsCommitMetadata getDetailsFromCache(int commitIndex) {
        VcsCommitMetadata details = myTopCommitsDetailsCache.get(commitIndex);
        if (details != null) {
            return details;
        }
        return myCommitDetailsGetter.getCommitDataIfAvailable(commitIndex);
    }

    
    private static Collection<CommitId> getFilteredDetailsFromTheVcs(
        Map<VirtualFile, VcsLogProvider> providers,
        VcsLogFilterCollection filterCollection,
        int maxCount
    ) throws VcsException {
        Set<VirtualFile> visibleRoots =
            VcsLogUtil.getAllVisibleRoots(providers.keySet(), filterCollection.getRootFilter(), filterCollection.getStructureFilter());

        Collection<CommitId> commits = new ArrayList<>();
        for (Map.Entry<VirtualFile, VcsLogProvider> entry : providers.entrySet()) {
            VirtualFile root = entry.getKey();

            if (!visibleRoots.contains(root) ||
                (filterCollection.getUserFilter() != null && filterCollection.getUserFilter().getUsers(root).isEmpty())) {
                // there is a structure or user filter, but it doesn't match this root
                continue;
            }

            VcsLogFilterCollection rootSpecificCollection = filterCollection;
            if (rootSpecificCollection.getStructureFilter() != null) {
                rootSpecificCollection = new VcsLogFilterCollectionImpl.VcsLogFilterCollectionBuilder(filterCollection)
                    .with(new VcsLogStructureFilterImpl(new HashSet<FilePath>(VcsLogUtil.getFilteredFilesForRoot(root, filterCollection))))
                    .build();
            }

            List<TimedVcsCommit> matchingCommits = entry.getValue().getCommitsMatchingFilter(root, rootSpecificCollection, maxCount);
            commits.addAll(ContainerUtil.map(matchingCommits, commit -> new CommitId(commit.getId(), root)));
        }

        return commits;
    }

    private @Nullable Set<Integer> getMatchedCommitIndex(@Nullable Collection<CommitId> commits) {
        if (commits == null) {
            return null;
        }

        return ContainerUtil.map2Set(commits, commitId -> myHashMap.getCommitIndex(commitId.getHash(), commitId.getRoot()));
    }

    private static class FilterResult {
        private final @Nullable Set<Integer> matchingCommits;
        private final boolean canRequestMore;
        
        private final CommitCountStage commitCount;

        private FilterResult(@Nullable Set<Integer> commits, boolean more, CommitCountStage count) {
            matchingCommits = commits;
            canRequestMore = more;
            commitCount = count;
        }
    }
}
