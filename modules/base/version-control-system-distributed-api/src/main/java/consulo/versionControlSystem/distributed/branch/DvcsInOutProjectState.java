// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.distributed.branch;

import consulo.versionControlSystem.FilePath;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Incoming and outgoing commit counters of every local branch, per repository root, and the time of the last fetch.
 */
public record DvcsInOutProjectState(Map<FilePath, Map<String, Integer>> incoming,
                                    Map<FilePath, Map<String, Integer>> outgoing,
                                    @Nullable Instant lastFetchTime) {
    public static final DvcsInOutProjectState EMPTY = new DvcsInOutProjectState(Map.of(), Map.of(), null);

    /**
     * @return incoming and outgoing counters for the local branch in the given repositories.
     */
    public DvcsInOutCountersInProject getCounters(String branchName, Collection<FilePath> repositories) {
        if (repositories.isEmpty()) {
            return DvcsInOutCountersInProject.EMPTY;
        }

        Map<FilePath, DvcsInOutCountersInRepo> reposState = new LinkedHashMap<>();
        for (FilePath repository : repositories) {
            Map<String, Integer> incomingInRepo = incoming.get(repository);
            Map<String, Integer> outgoingInRepo = outgoing.get(repository);
            Integer incomingCount = incomingInRepo == null ? null : incomingInRepo.get(branchName);
            Integer outgoingCount = outgoingInRepo == null ? null : outgoingInRepo.get(branchName);
            if (incomingCount != null || outgoingCount != null) {
                reposState.put(repository, new DvcsInOutCountersInRepo(incomingCount, outgoingCount));
            }
        }

        return reposState.isEmpty() ? DvcsInOutCountersInProject.EMPTY : new DvcsInOutCountersInProject(reposState);
    }
}
