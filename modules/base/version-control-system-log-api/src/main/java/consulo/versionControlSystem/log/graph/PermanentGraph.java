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
package consulo.versionControlSystem.log.graph;

import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import org.jspecify.annotations.Nullable;

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
    
    static <CommitId> PermanentGraph<CommitId> newInstance(
        List<? extends GraphCommit<CommitId>> graphCommits,
        GraphColorManager<CommitId> graphColorManager,
        Set<CommitId> branchesCommitId
    ) {
        Application application = Application.get();
        PermanentGraphFactory factory = application.getInstance(PermanentGraphFactory.class);
        return factory.newInstance(graphCommits, graphColorManager, branchesCommitId);
    }

    
    VisibleGraph<Id> createVisibleGraph(
        SortType sortType,
        @Nullable Set<Id> headsOfVisibleBranches,
        @Nullable Set<Id> matchedCommits
    );

    
    List<GraphCommit<Id>> getAllCommits();

    
    List<Id> getChildren(Id commit);

    
    Set<Id> getContainingBranches(Id commit);

    
    Predicate<Id> getContainedInBranchCondition(Collection<Id> currentBranchHead);

    enum SortType {
        Normal(VersionControlSystemLogLocalize.graphSortOffName(), VersionControlSystemLogLocalize.graphSortOffDescription()),
        Bek(VersionControlSystemLogLocalize.graphSortStandardName(), VersionControlSystemLogLocalize.graphSortStandardDescription()),
        LinearBek(VersionControlSystemLogLocalize.graphSortLinearName(), VersionControlSystemLogLocalize.graphSortLinearDescription());

        
        private final LocalizeValue myName;
        
        private final LocalizeValue myDescription;

        SortType(LocalizeValue name, LocalizeValue description) {
            myName = name;
            myDescription = description;
        }

        
        public LocalizeValue getName() {
            return myName;
        }

        
        public LocalizeValue getDescription() {
            return myDescription;
        }
    }
}
