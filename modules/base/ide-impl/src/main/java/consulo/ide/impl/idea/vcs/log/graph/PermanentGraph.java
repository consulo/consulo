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
package consulo.ide.impl.idea.vcs.log.graph;

import consulo.application.Application;
import consulo.versionControlSystem.log.graph.GraphColorManager;
import consulo.versionControlSystem.log.graph.GraphCommit;
import consulo.versionControlSystem.log.graph.VisibleGraph;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * PermanentGraph is created once per repository, and forever until the log is refreshed. <br/>
 * An instance can be achieved by {@link PermanentGraph#newInstance(List, GraphColorManager, Set)}. <br/>
 * This graph contains all commits in the log and may occupy a lot.
 *
 * @see VisibleGraph
 */
public interface PermanentGraph<Id> {
    @Nonnull
    static <CommitId> PermanentGraph<CommitId> newInstance(
        @Nonnull List<? extends GraphCommit<CommitId>> graphCommits,
        @Nonnull GraphColorManager<CommitId> graphColorManager,
        @Nonnull Set<CommitId> branchesCommitId
    ) {
        Application application = Application.get();
        PermanentGraphFactory factory = application.getInstance(PermanentGraphFactory.class);
        return factory.newInstance(graphCommits, graphColorManager, branchesCommitId);
    }

    @Nonnull
    VisibleGraph<Id> createVisibleGraph(
        @Nonnull SortType sortType,
        @Nullable Set<Id> headsOfVisibleBranches,
        @Nullable Set<Id> matchedCommits
    );

    @Nonnull
    List<GraphCommit<Id>> getAllCommits();

    @Nonnull
    List<Id> getChildren(@Nonnull Id commit);

    @Nonnull
    Set<Id> getContainingBranches(@Nonnull Id commit);

    @Nonnull
    Predicate<Id> getContainedInBranchCondition(@Nonnull Collection<Id> currentBranchHead);

    enum SortType {
        Normal("Off", "Sort commits topologically and by date"),
        Bek("Standard", "In case of merge show incoming commits first (directly below merge commit)"),
        LinearBek("Linear", "In case of merge show incoming commits on top of main branch commits as if they were rebased");

        @Nonnull
        private final String myPresentation;
        @Nonnull
        private final String myDescription;

        SortType(@Nonnull String presentation, @Nonnull String description) {
            myPresentation = presentation;
            myDescription = description;
        }

        @Nonnull
        public String getName() {
            return myPresentation;
        }

        @Nonnull
        public String getDescription() {
            return myDescription;
        }
    }
}
