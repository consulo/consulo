// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff;

import consulo.collaboration.localize.CollaborationToolsLocalize;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

public enum DiscussionsViewOption {
    ALL,
    UNRESOLVED_ONLY,
    DONT_SHOW;

    public @Nls @Nonnull String toActionName() {
        return switch (this) {
            case ALL -> CollaborationToolsLocalize.reviewDiffDiscussionsViewOptionAll().get();
            case UNRESOLVED_ONLY -> CollaborationToolsLocalize.reviewDiffDiscussionsViewOptionUnresolvedOnly().get();
            case DONT_SHOW -> CollaborationToolsLocalize.reviewDiffDiscussionsViewOptionDoNotShow().get();
        };
    }
}
