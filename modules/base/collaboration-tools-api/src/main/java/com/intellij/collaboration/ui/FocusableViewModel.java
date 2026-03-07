// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import kotlinx.coroutines.flow.Flow;
import kotlin.Unit;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;

/**
 * Represents any type of note, discussion, or comment that can be displayed within the IDE as an inlay.
 */
@ApiStatus.Internal
public interface FocusableViewModel {
    @Nonnull
    Flow<Unit> getFocusRequests();

    void requestFocus();
}
