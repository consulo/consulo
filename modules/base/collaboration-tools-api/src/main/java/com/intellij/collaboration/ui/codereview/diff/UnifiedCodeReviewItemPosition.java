// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff;

import com.intellij.collaboration.util.RefComparisonChange;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * This class indicates the position of an object inside a diff as a single ordered number.
 * In practice, the line number is always the right-side mapping of the location.
 * If a left-side location has been removed, the right-side mapping is estimated to be the first line above the changed section.
 * <p>
 * Line numbers are supposed to be 0-indexed.
 */
@ApiStatus.Internal
public final class UnifiedCodeReviewItemPosition {
    private final @Nonnull RefComparisonChange change;
    private final int leftLine;
    private final int rightLine;

    public UnifiedCodeReviewItemPosition(@Nonnull RefComparisonChange change, int leftLine, int rightLine) {
        this.change = change;
        this.leftLine = leftLine;
        this.rightLine = rightLine;
    }

    public @Nonnull RefComparisonChange getChange() {
        return change;
    }

    public int getLeftLine() {
        return leftLine;
    }

    public int getRightLine() {
        return rightLine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnifiedCodeReviewItemPosition that)) {
            return false;
        }
        return leftLine == that.leftLine &&
            rightLine == that.rightLine &&
            Objects.equals(change, that.change);
    }

    @Override
    public int hashCode() {
        return Objects.hash(change, leftLine, rightLine);
    }

    @Override
    public String toString() {
        return "UnifiedCodeReviewItemPosition(" +
            "change=" + change +
            ", leftLine=" + leftLine +
            ", rightLine=" + rightLine +
            ')';
    }
}
