// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import com.intellij.collaboration.util.RefComparisonChange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public final class CodeReviewChangeList {
    private final @Nullable String commitSha;
    private final @Nonnull List<RefComparisonChange> changes;

    public CodeReviewChangeList(@Nullable String commitSha, @Nonnull List<RefComparisonChange> changes) {
        this.commitSha = commitSha;
        this.changes = changes;
    }

    public @Nullable String getCommitSha() {
        return commitSha;
    }

    public @Nonnull List<RefComparisonChange> getChanges() {
        return changes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CodeReviewChangeList that = (CodeReviewChangeList) o;
        return Objects.equals(commitSha, that.commitSha) && Objects.equals(changes, that.changes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commitSha, changes);
    }

    @Override
    public String toString() {
        return "CodeReviewChangeList(commitSha=" + commitSha + ", changes=" + changes + ')';
    }
}
