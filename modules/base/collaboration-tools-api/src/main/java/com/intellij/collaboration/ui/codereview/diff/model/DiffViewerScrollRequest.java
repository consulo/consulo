// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation;
import com.intellij.diff.util.DiffUserDataKeysEx.ScrollToPolicy;
import jakarta.annotation.Nonnull;

public sealed interface DiffViewerScrollRequest permits DiffViewerLineScrollRequest, DiffViewerChangeScrollRequest {
    static @Nonnull DiffViewerScrollRequest toLine(@Nonnull DiffLineLocation location) {
        return new DiffViewerLineScrollRequest(location);
    }

    static @Nonnull DiffViewerScrollRequest toFirstChange() {
        return new DiffViewerChangeScrollRequest(ScrollToPolicy.FIRST_CHANGE);
    }

    static @Nonnull DiffViewerScrollRequest toLastChange() {
        return new DiffViewerChangeScrollRequest(ScrollToPolicy.LAST_CHANGE);
    }
}
