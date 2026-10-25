// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.versionControlSystem.review.diff.DiffLineLocation;

public sealed interface DiffViewerScrollRequest permits DiffViewerLineScrollRequest, DiffViewerChangeScrollRequest {
    static DiffViewerScrollRequest toLine(DiffLineLocation location) {
        return new DiffViewerLineScrollRequest(location);
    }

    static DiffViewerScrollRequest toFirstChange() {
        return new DiffViewerChangeScrollRequest(ScrollToPolicy.FIRST_CHANGE);
    }

    static DiffViewerScrollRequest toLastChange() {
        return new DiffViewerChangeScrollRequest(ScrollToPolicy.LAST_CHANGE);
    }
}
