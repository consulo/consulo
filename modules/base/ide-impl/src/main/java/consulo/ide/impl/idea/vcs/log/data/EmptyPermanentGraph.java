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

import consulo.ide.impl.idea.vcs.log.graph.PermanentGraph;
import consulo.util.lang.function.Conditions;
import consulo.versionControlSystem.log.graph.GraphCommit;
import consulo.versionControlSystem.log.graph.VisibleGraph;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class EmptyPermanentGraph implements PermanentGraph<Integer> {

    private static final PermanentGraph<Integer> INSTANCE = new EmptyPermanentGraph();

    @Nonnull
    public static PermanentGraph<Integer> getInstance() {
        return INSTANCE;
    }

    @Nonnull
    @Override
    public VisibleGraph<Integer> createVisibleGraph(
        @Nonnull SortType sortType,
        @Nullable Set<Integer> headsOfVisibleBranches,
        @Nullable Set<Integer> filter
    ) {
        return EmptyVisibleGraph.getInstance();
    }

    @Nonnull
    @Override
    public List<GraphCommit<Integer>> getAllCommits() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<Integer> getChildren(@Nonnull Integer commit) {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Set<Integer> getContainingBranches(@Nonnull Integer commit) {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Predicate<Integer> getContainedInBranchCondition(@Nonnull Collection<Integer> currentBranchHead) {
        return Conditions.alwaysFalse();
    }
}
