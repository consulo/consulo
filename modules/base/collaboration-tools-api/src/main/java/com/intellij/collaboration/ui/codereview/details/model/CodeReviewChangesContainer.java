// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import com.intellij.collaboration.util.RefComparisonChange;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeReviewChangesContainer {
    private final @Nonnull List<RefComparisonChange> summaryChanges;
    private final @Nonnull List<String> commits;
    private final @Nonnull Map<String, List<RefComparisonChange>> changesByCommits;
    private final @Nonnull Map<RefComparisonChange, String> commitsByChange;

    public CodeReviewChangesContainer(
        @Nonnull List<RefComparisonChange> summaryChanges,
        @Nonnull List<String> commits,
        @Nonnull Map<String, List<RefComparisonChange>> changesByCommits
    ) {
        this.summaryChanges = summaryChanges;
        this.commits = commits;
        this.changesByCommits = changesByCommits;

        Map<RefComparisonChange, String> map = new HashMap<>();
        for (Map.Entry<String, List<RefComparisonChange>> entry : changesByCommits.entrySet()) {
            for (RefComparisonChange change : entry.getValue()) {
                map.put(change, entry.getKey());
            }
        }
        this.commitsByChange = map;
    }

    public @Nonnull List<RefComparisonChange> getSummaryChanges() {
        return summaryChanges;
    }

    public @Nonnull List<String> getCommits() {
        return commits;
    }

    public @Nonnull Map<String, List<RefComparisonChange>> getChangesByCommits() {
        return changesByCommits;
    }

    public @Nonnull Map<RefComparisonChange, String> getCommitsByChange() {
        return commitsByChange;
    }
}
