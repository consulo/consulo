// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import com.intellij.collaboration.util.ComputedResult;
import consulo.diff.request.DiffRequest;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nullable;

/**
 * A viewmodel for a diff between two changes.
 * Implementations should provide an equals/hashCode implementation.
 */
public interface AsyncDiffViewModel {
    StateFlow<@Nullable ComputedResult<DiffRequest>> getRequest();

    void reloadRequest();
}
