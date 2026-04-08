// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import jakarta.annotation.Nonnull;

import java.util.Objects;

final class DiffViewerChangeScrollRequest implements DiffViewerScrollRequest {
    private final @Nonnull ScrollToPolicy policy;

    DiffViewerChangeScrollRequest(@Nonnull ScrollToPolicy policy) {
        this.policy = policy;
    }

    public @Nonnull ScrollToPolicy getPolicy() {
        return policy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DiffViewerChangeScrollRequest that)) {
            return false;
        }
        return policy == that.policy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(policy);
    }

    @Override
    public String toString() {
        return "DiffViewerChangeScrollRequest(policy=" + policy + ')';
    }
}
