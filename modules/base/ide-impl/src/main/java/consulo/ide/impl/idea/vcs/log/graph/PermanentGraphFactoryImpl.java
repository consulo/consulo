/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.vcs.log.graph;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.PermanentGraphImpl;
import consulo.ide.impl.idea.vcs.log.graph.impl.permanent.*;
import consulo.versionControlSystem.log.graph.GraphColorManager;
import consulo.versionControlSystem.log.graph.GraphCommit;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2025-08-29
 */
@ServiceImpl
@Singleton
public class PermanentGraphFactoryImpl implements PermanentGraphFactory {

    private static class NotLoadedCommitsIdsGenerator<CommitId> implements NotNullFunction<CommitId, Integer> {
        @Nonnull
        private final Map<Integer, CommitId> myNotLoadedCommits = new HashMap<>();

        @Nonnull
        @Override
        public Integer apply(CommitId dom) {
            int nodeId = -(myNotLoadedCommits.size() + 2);
            myNotLoadedCommits.put(nodeId, dom);
            return nodeId;
        }

        @Nonnull
        public Map<Integer, CommitId> getNotLoadedCommits() {
            return myNotLoadedCommits;
        }
    }

    @Override
    public <CommitId> PermanentGraph<CommitId> newInstance(@Nonnull List<? extends GraphCommit<CommitId>> graphCommits,
                                                           @Nonnull GraphColorManager<CommitId> graphColorManager,
                                                           @Nonnull Set<CommitId> branchesCommitId) {
        PermanentLinearGraphBuilder<CommitId> permanentLinearGraphBuilder = PermanentLinearGraphBuilder.newInstance(graphCommits);
        NotLoadedCommitsIdsGenerator<CommitId> idsGenerator = new NotLoadedCommitsIdsGenerator<>();
        PermanentLinearGraphImpl linearGraph = permanentLinearGraphBuilder.build(idsGenerator);

        PermanentCommitsInfoImpl<CommitId> commitIdPermanentCommitsInfo =
            PermanentCommitsInfoImpl.newInstance(graphCommits, idsGenerator.getNotLoadedCommits());

        GraphLayoutImpl permanentGraphLayout = GraphLayoutBuilder.build(linearGraph, (nodeIndex1, nodeIndex2) -> {
            CommitId commitId1 = commitIdPermanentCommitsInfo.getCommitId(nodeIndex1);
            CommitId commitId2 = commitIdPermanentCommitsInfo.getCommitId(nodeIndex2);
            return graphColorManager.compareHeads(commitId2, commitId1);
        });

        return new PermanentGraphImpl<>(
            linearGraph,
            permanentGraphLayout,
            commitIdPermanentCommitsInfo,
            graphColorManager,
            branchesCommitId
        );
    }
}
