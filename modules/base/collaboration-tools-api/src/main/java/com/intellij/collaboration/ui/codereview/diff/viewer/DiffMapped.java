// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.viewer;

import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation;
import kotlinx.coroutines.flow.Flow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface DiffMapped {
    @Nonnull
    Flow<@Nullable DiffLineLocation> getLocation();

    @Nonnull
    Flow<Boolean> getIsVisible();
}
