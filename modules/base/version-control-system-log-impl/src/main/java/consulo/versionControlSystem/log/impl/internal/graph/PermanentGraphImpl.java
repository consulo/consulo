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

package consulo.versionControlSystem.log.impl.internal.graph;

import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.lang.lazy.LazyValue;
import consulo.versionControlSystem.log.graph.*;
import consulo.versionControlSystem.log.impl.internal.graph.bek.BekBaseController;
import consulo.versionControlSystem.log.impl.internal.graph.bek.BekIntMap;
import consulo.versionControlSystem.log.impl.internal.graph.bek.BekSorter;
import consulo.versionControlSystem.log.impl.internal.graph.bek.LinearBekController;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

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

    
    private final PermanentCommitsInfoImpl<CommitId> myPermanentCommitsInfo;
    
    private final PermanentLinearGraphImpl myPermanentLinearGraph;
    
    private final GraphLayoutImpl myPermanentGraphLayout;
    
    private final GraphColorManager<CommitId> myGraphColorManager;
    
    private final Set<Integer> myBranchNodeIds;
    
    private final ReachableNodes myReachableNodes;
    
    private final Supplier<BekIntMap> myBekIntMap;

    public PermanentGraphImpl(
        PermanentLinearGraphImpl permanentLinearGraph,
        GraphLayoutImpl permanentGraphLayout,
        PermanentCommitsInfoImpl<CommitId> permanentCommitsInfo,
        GraphColorManager<CommitId> graphColorManager,
        Set<CommitId> branchesCommitId
    ) {
        myPermanentGraphLayout = permanentGraphLayout;
        myPermanentCommitsInfo = permanentCommitsInfo;
        myPermanentLinearGraph = permanentLinearGraph;
        myGraphColorManager = graphColorManager;
        myBranchNodeIds = permanentCommitsInfo.convertToNodeIds(branchesCommitId);
        myReachableNodes = new ReachableNodes(LinearGraphUtils.asLiteLinearGraph(permanentLinearGraph));
        myBekIntMap = LazyValue.notNull(() -> BekSorter.createBekMap(
            myPermanentLinearGraph,
            myPermanentGraphLayout,
            myPermanentCommitsInfo.getTimestampGetter()
        ));
    }

    
    @Override
    public VisibleGraph<CommitId> createVisibleGraph(
        SortType sortType,
        @Nullable Set<CommitId> visibleHeads,
        @Nullable Set<CommitId> matchingCommits
    ) {
        CascadeController baseController = switch (sortType) {
            case Normal -> new BaseController(this);
            case Bek -> new BekBaseController(this, myBekIntMap.get());
            case LinearBek -> new LinearBekController(new BekBaseController(this, myBekIntMap.get()), this);
        };

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

    
    @Override
    public List<CommitId> getChildren(CommitId commit) {
        int commitIndex = myPermanentCommitsInfo.getNodeId(commit);
        return myPermanentCommitsInfo.convertToCommitIdList(LinearGraphUtils.getUpNodes(myPermanentLinearGraph, commitIndex));
    }

    
    @Override
    public Set<CommitId> getContainingBranches(CommitId commit) {
        int commitIndex = myPermanentCommitsInfo.getNodeId(commit);
        return myPermanentCommitsInfo.convertToCommitIdSet(myReachableNodes.getContainingBranches(commitIndex, myBranchNodeIds));
    }

    
    @Override
    public Predicate<CommitId> getContainedInBranchCondition(Collection<CommitId> heads) {
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
    
    public PermanentCommitsInfoImpl<CommitId> getPermanentCommitsInfo() {
        return myPermanentCommitsInfo;
    }

    @Override
    
    public PermanentLinearGraphImpl getLinearGraph() {
        return myPermanentLinearGraph;
    }

    @Override
    
    public GraphLayoutImpl getPermanentGraphLayout() {
        return myPermanentGraphLayout;
    }

    @Override
    
    public Set<Integer> getBranchNodeIds() {
        return myBranchNodeIds;
    }
}
