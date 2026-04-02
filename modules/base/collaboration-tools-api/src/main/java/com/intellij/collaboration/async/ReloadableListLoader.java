// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import kotlin.coroutines.Continuation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ApiStatus.Internal
public interface ReloadableListLoader {
    /**
     * Empty any existing list, cancel any existing refresh requests.
     * Stop tracking all server-side data. Basically, reset this list.
     * <p>
     * Then, load the first 'page' of data.
     */
    @Nullable
    Object reload(@Nonnull Continuation<? super kotlin.Unit> continuation);

    /**
     * Poll for changes of the loaded data and fetch added data at
     * the 'end' of the list.
     */
    @Nullable
    Object refresh(@Nonnull Continuation<? super kotlin.Unit> continuation);
}
