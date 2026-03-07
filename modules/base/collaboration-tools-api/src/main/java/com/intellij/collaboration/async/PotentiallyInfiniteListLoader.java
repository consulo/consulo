// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import kotlin.coroutines.Continuation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ApiStatus.Internal
public interface PotentiallyInfiniteListLoader {
    /**
     * Loads a new 'page' worth of items and adds them to the list.
     */
    @Nullable
    Object loadMore(@Nonnull Continuation<? super kotlin.Unit> continuation);

    /**
     * Loads all 'pages' of items and adds them to the list.
     */
    @Nullable
    Object loadAll(@Nonnull Continuation<? super kotlin.Unit> continuation);
}
