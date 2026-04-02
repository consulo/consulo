// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import java.util.Objects;

public final class CodeReviewChangeDetails {
    private final boolean isRead;
    private final int discussions;

    public CodeReviewChangeDetails(boolean isRead, int discussions) {
        this.isRead = isRead;
        this.discussions = discussions;
    }

    public boolean isRead() {
        return isRead;
    }

    public int getDiscussions() {
        return discussions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CodeReviewChangeDetails that = (CodeReviewChangeDetails) o;
        return isRead == that.isRead && discussions == that.discussions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isRead, discussions);
    }

    @Override
    public String toString() {
        return "CodeReviewChangeDetails(isRead=" + isRead + ", discussions=" + discussions + ')';
    }
}
