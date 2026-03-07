// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation;
import jakarta.annotation.Nonnull;

import java.util.Objects;

final class DiffViewerLineScrollRequest implements DiffViewerScrollRequest {
    private final @Nonnull DiffLineLocation location;

    DiffViewerLineScrollRequest(@Nonnull DiffLineLocation location) {
        this.location = location;
    }

    public @Nonnull DiffLineLocation getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DiffViewerLineScrollRequest that)) {
            return false;
        }
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return "DiffViewerLineScrollRequest(location=" + location + ')';
    }
}
