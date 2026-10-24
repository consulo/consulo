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

import consulo.application.util.DateFormatUtil;
import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Incoming and outgoing commit counters of one branch in a set of repositories, keyed by repository root.
 */
public record DvcsInOutCountersInProject(Map<FilePath, DvcsInOutCountersInRepo> inOutRepoState) {
    public static final DvcsInOutCountersInProject EMPTY = new DvcsInOutCountersInProject(Map.of());

    public boolean hasOutgoing() {
        return inOutRepoState.values().stream().anyMatch(DvcsInOutCountersInRepo::hasOutgoing);
    }

    public boolean hasIncoming() {
        return inOutRepoState.values().stream().anyMatch(DvcsInOutCountersInRepo::hasIncoming);
    }

    public boolean hasUnfetched() {
        return inOutRepoState.values().stream().anyMatch(DvcsInOutCountersInRepo::hasUnfetched);
    }

    public int totalOutgoing() {
        return inOutRepoState.values().stream().mapToInt(it -> it.outgoing() == null ? 0 : it.outgoing()).sum();
    }

    public int totalIncoming() {
        return inOutRepoState.values().stream().mapToInt(it -> it.incoming() == null ? 0 : it.incoming()).sum();
    }

    public int reposWithIncoming() {
        return (int) inOutRepoState.values().stream().filter(DvcsInOutCountersInRepo::hasIncoming).count();
    }

    public int reposWithOutgoing() {
        return (int) inOutRepoState.values().stream().filter(DvcsInOutCountersInRepo::hasOutgoing).count();
    }

    public int reposWithUnfetched() {
        return (int) inOutRepoState.values().stream().filter(DvcsInOutCountersInRepo::hasUnfetched).count();
    }

    public Set<FilePath> repositories() {
        return inOutRepoState.keySet();
    }

    public boolean isEmpty() {
        return inOutRepoState.isEmpty();
    }

    public LocalizeValue calcTooltip() {
        return calcTooltip(null);
    }

    public LocalizeValue calcTooltip(@Nullable Instant lastFetchTime) {
        if (isEmpty()) {
            return LocalizeValue.empty();
        }

        Set<FilePath> repositories = repositories();

        List<LocalizeValue> lines = new ArrayList<>();

        int totalIncoming = totalIncoming();
        int totalOutgoing = totalOutgoing();

        if (repositories.size() == 1) {
            LocalizeValue message = null;
            if (hasUnfetched()) {
                message = DistributedVcsLocalize.branchesTooltipSomeIncomingCommitsNotFetched();
            }
            else if (totalIncoming != 0 && totalOutgoing != 0) {
                message = DistributedVcsLocalize.branchesTooltipIncomingAndOutgoingCommits(totalIncoming, totalOutgoing);
            }
            else if (totalIncoming != 0) {
                message = DistributedVcsLocalize.branchesTooltipNumberIncomingCommits(totalIncoming);
            }
            else if (totalOutgoing != 0) {
                message = DistributedVcsLocalize.branchesTooltipNumberOutgoingCommits(totalOutgoing);
            }
            if (message != null) {
                lines.add(message);
            }

            if (lastFetchTime != null) {
                lines.add(DistributedVcsLocalize.branchesTooltipLastFetch(DateFormatUtil.formatPrettyDateTime(lastFetchTime.toEpochMilli())));
            }
        }
        else {
            if (totalIncoming != 0) {
                lines.add(DistributedVcsLocalize.branchesTooltipNumberIncomingCommitsInRepositories(totalIncoming, reposWithIncoming()));
            }
            else if (hasUnfetched()) {
                lines.add(DistributedVcsLocalize.branchesTooltipSomeIncomingCommitsNotFetched());
            }

            if (totalOutgoing != 0) {
                lines.add(DistributedVcsLocalize.branchesTooltipNumberOutgoingCommitsInRepositories(totalOutgoing, reposWithOutgoing()));
            }
        }

        return lines.isEmpty() ? LocalizeValue.empty() : LocalizeValue.joinWithSeparator(LocalizeValue.of("\n"), lines.toArray(LocalizeValue[]::new));
    }
}
