// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public final class CodeReviewBranches {
    private final @Nonnull String source;
    private final @Nonnull String target;
    private final boolean hasRemoteBranch;

    public CodeReviewBranches(@Nonnull String source, @Nonnull String target, boolean hasRemoteBranch) {
        this.source = source;
        this.target = target;
        this.hasRemoteBranch = hasRemoteBranch;
    }

    // For compatibility
    public CodeReviewBranches(@Nonnull String source, @Nonnull String target) {
        this(source, target, true);
    }

    public @Nonnull String getSource() {
        return source;
    }

    public @Nonnull String getTarget() {
        return target;
    }

    public boolean getHasRemoteBranch() {
        return hasRemoteBranch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CodeReviewBranches that = (CodeReviewBranches) o;
        return hasRemoteBranch == that.hasRemoteBranch &&
            Objects.equals(source, that.source) &&
            Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, hasRemoteBranch);
    }

    @Override
    public String toString() {
        return "CodeReviewBranches(" +
            "source='" + source + '\'' +
            ", target='" + target + '\'' +
            ", hasRemoteBranch=" + hasRemoteBranch +
            ')';
    }
}
