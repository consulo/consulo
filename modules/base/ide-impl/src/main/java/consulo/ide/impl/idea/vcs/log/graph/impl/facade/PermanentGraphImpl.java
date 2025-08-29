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

package consulo.ide.impl.idea.vcs.log.graph.impl.facade;


import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import consulo.ide.impl.idea.vcs.log.graph.GraphCommitImpl;
import consulo.ide.impl.idea.vcs.log.graph.PermanentGraph;
import consulo.ide.impl.idea.vcs.log.graph.api.permanent.PermanentGraphInfo;
import consulo.ide.impl.idea.vcs.log.graph.collapsing.BranchFilterController;
import consulo.ide.impl.idea.vcs.log.graph.collapsing.CollapsedController;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.bek.BekIntMap;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.bek.BekSorter;
import consulo.ide.impl.idea.vcs.log.graph.impl.permanent.GraphLayoutImpl;
import consulo.ide.impl.idea.vcs.log.graph.impl.permanent.PermanentCommitsInfoImpl;
import consulo.ide.impl.idea.vcs.log.graph.impl.permanent.PermanentLinearGraphImpl;
import consulo.ide.impl.idea.vcs.log.graph.linearBek.LinearBekController;
import consulo.ide.impl.idea.vcs.log.graph.utils.LinearGraphUtils;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.versionControlSystem.log.graph.GraphColorManager;
import consulo.versionControlSystem.log.graph.GraphCommit;
import consulo.versionControlSystem.log.graph.VisibleGraph;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class PermanentGraphImpl<CommitId> implements PermanentGraph<CommitId>, PermanentGraphInfo<CommitId> {
    private static class IntContainedInBranchCondition<CommitId> implements Predicate<CommitId> {
        private final IntSet myBranchNodes;

        public IntContainedInBranchCondition(IntSet branchNodes) {
            myBranchNodes = branchNodes;
        }

        @Override
        public boolean test(CommitId commitId) {
            return myBranchNodes.contains((Integer) commitId);
        }
    }

    private static class ContainedInBranchCondition<CommitId> implements Predicate<CommitId> {
        private final Set<CommitId> myBranchNodes;

        public ContainedInBranchCondition(Set<CommitId> branchNodes) {
            myBranchNodes = branchNodes;
        }

        @Override
        public boolean test(CommitId commitId) {
            return myBranchNodes.contains(commitId);
        }
    }

    @Nonnull
    private final PermanentCommitsInfoImpl<CommitId> myPermanentCommitsInfo;
    @Nonnull
    private final PermanentLinearGraphImpl myPermanentLinearGraph;
    @Nonnull
    private final GraphLayoutImpl myPermanentGraphLayout;
    @Nonnull
    private final GraphColorManager<CommitId> myGraphColorManager;
    @Nonnull
    private final Set<Integer> myBranchNodeIds;
    @Nonnull
    private final ReachableNodes myReachableNodes;
    @Nonnull
    private final Supplier<BekIntMap> myBekIntMap;

    public PermanentGraphImpl(
        @Nonnull PermanentLinearGraphImpl permanentLinearGraph,
        @Nonnull GraphLayoutImpl permanentGraphLayout,
        @Nonnull PermanentCommitsInfoImpl<CommitId> permanentCommitsInfo,
        @Nonnull GraphColorManager<CommitId> graphColorManager,
        @Nonnull Set<CommitId> branchesCommitId
    ) {
        myPermanentGraphLayout = permanentGraphLayout;
        myPermanentCommitsInfo = permanentCommitsInfo;
        myPermanentLinearGraph = permanentLinearGraph;
        myGraphColorManager = graphColorManager;
        myBranchNodeIds = permanentCommitsInfo.convertToNodeIds(branchesCommitId);
        myReachableNodes = new ReachableNodes(LinearGraphUtils.asLiteLinearGraph(permanentLinearGraph));
        myBekIntMap = Suppliers.memoize(() -> BekSorter.createBekMap(
            myPermanentLinearGraph,
            myPermanentGraphLayout,
            myPermanentCommitsInfo.getTimestampGetter()
        ));
    }

    @Nonnull
    @Override
    public VisibleGraph<CommitId> createVisibleGraph(
        @Nonnull SortType sortType,
        @Nullable Set<CommitId> visibleHeads,
        @Nullable Set<CommitId> matchingCommits
    ) {
        CascadeController baseController;
        if (sortType == SortType.Normal) {
            baseController = new BaseController(this);
        }
        else if (sortType == SortType.LinearBek) {
            baseController = new LinearBekController(new BekBaseController(this, myBekIntMap.get()), this);
        }
        else {
            baseController = new BekBaseController(this, myBekIntMap.get());
        }

        LinearGraphController controller;
        if (matchingCommits != null) {
            controller = new FilteredController(baseController, this, myPermanentCommitsInfo.convertToNodeIds(matchingCommits));
        }
        else if (sortType == SortType.LinearBek) {
            if (visibleHeads != null) {
                controller = new BranchFilterController(baseController, this, myPermanentCommitsInfo.convertToNodeIds(visibleHeads, true));
            }
            else {
                controller = baseController;
            }
        }
        else {
            Set<Integer> idOfVisibleBranches = null;
            if (visibleHeads != null) {
                idOfVisibleBranches = myPermanentCommitsInfo.convertToNodeIds(visibleHeads, true);
            }
            controller = new CollapsedController(baseController, this, idOfVisibleBranches);
        }

        return new VisibleGraphImpl<>(controller, this, myGraphColorManager);
    }

    @Nonnull
    @Override
    public List<GraphCommit<CommitId>> getAllCommits() {
        List<GraphCommit<CommitId>> result = new ArrayList<>();
        for (int index = 0; index < myPermanentLinearGraph.nodesCount(); index++) {
            CommitId commitId = myPermanentCommitsInfo.getCommitId(index);
            List<Integer> downNodes = LinearGraphUtils.getDownNodesIncludeNotLoad(myPermanentLinearGraph, index);
            List<CommitId> parentsCommitIds = myPermanentCommitsInfo.convertToCommitIdList(downNodes);
            GraphCommit<CommitId> graphCommit =
                new GraphCommitImpl<>(commitId, parentsCommitIds, myPermanentCommitsInfo.getTimestamp(index));
            result.add(graphCommit);
        }

        return result;
    }

    @Nonnull
    @Override
    public List<CommitId> getChildren(@Nonnull CommitId commit) {
        int commitIndex = myPermanentCommitsInfo.getNodeId(commit);
        return myPermanentCommitsInfo.convertToCommitIdList(LinearGraphUtils.getUpNodes(myPermanentLinearGraph, commitIndex));
    }

    @Nonnull
    @Override
    public Set<CommitId> getContainingBranches(@Nonnull CommitId commit) {
        int commitIndex = myPermanentCommitsInfo.getNodeId(commit);
        return myPermanentCommitsInfo.convertToCommitIdSet(myReachableNodes.getContainingBranches(commitIndex, myBranchNodeIds));
    }

    @Nonnull
    @Override
    public Predicate<CommitId> getContainedInBranchCondition(@Nonnull Collection<CommitId> heads) {
        List<Integer> headIds = ContainerUtil.map(heads, myPermanentCommitsInfo::getNodeId);
        if (!heads.isEmpty() && ContainerUtil.getFirstItem(heads) instanceof Integer) {
            IntSet branchNodes = IntSets.newHashSet();
            myReachableNodes.walk(headIds, node -> branchNodes.add((Integer)myPermanentCommitsInfo.getCommitId(node)));
            return new IntContainedInBranchCondition<>(branchNodes);
        }
        else {
            Set<CommitId> branchNodes = new HashSet<>();
            myReachableNodes.walk(headIds, node -> branchNodes.add(myPermanentCommitsInfo.getCommitId(node)));
            return new ContainedInBranchCondition<>(branchNodes);
        }
    }

    @Override
    @Nonnull
    public PermanentCommitsInfoImpl<CommitId> getPermanentCommitsInfo() {
        return myPermanentCommitsInfo;
    }

    @Override
    @Nonnull
    public PermanentLinearGraphImpl getLinearGraph() {
        return myPermanentLinearGraph;
    }

    @Override
    @Nonnull
    public GraphLayoutImpl getPermanentGraphLayout() {
        return myPermanentGraphLayout;
    }

    @Override
    @Nonnull
    public Set<Integer> getBranchNodeIds() {
        return myBranchNodeIds;
    }
}
