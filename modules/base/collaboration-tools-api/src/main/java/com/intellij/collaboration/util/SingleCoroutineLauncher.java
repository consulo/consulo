// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import com.intellij.collaboration.async.CoroutineUtil;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlin.coroutines.Continuation;

public final class SingleCoroutineLauncher {
    private final @Nonnull CoroutineScope cs;
    private final @Nonnull MutableStateFlow<UUID> currentTaskKey;
    private @Nullable Job currentJob;
    private final @Nonnull StateFlow<Boolean> busy;

    public SingleCoroutineLauncher(@Nonnull CoroutineScope cs) {
        this.cs = cs;
        this.currentTaskKey = StateFlowKt.MutableStateFlow(null);
        this.busy = CoroutineUtil.mapState(currentTaskKey, cs, Objects::nonNull);
    }

    public @Nonnull StateFlow<Boolean> getBusy() {
        return busy;
    }

    public void launch(
        @Nonnull CoroutineContext context,
        @Nonnull CoroutineStart start,
        @Nonnull Function2<? super CoroutineScope, ? super Continuation<? super kotlin.Unit>, ? extends Object> block
    ) {
        UUID key = UUID.randomUUID();
        if (!currentTaskKey.compareAndSet(null, key)) {
            return;
        }
        currentJob = kotlinx.coroutines.BuildersKt.launch(cs, context, start, (scope, continuation) -> {
            try {
                return block.invoke(scope, continuation);
            }
            finally {
                currentJob = null;
                currentTaskKey.compareAndSet(key, null);
            }
        });
    }

    public void launch(@Nonnull Function2<? super CoroutineScope, ? super Continuation<? super kotlin.Unit>, ? extends Object> block) {
        launch(EmptyCoroutineContext.INSTANCE, CoroutineStart.DEFAULT, block);
    }

    public void cancel() {
        Job job = currentJob;
        if (job != null) {
            job.cancel(null);
        }
        currentJob = null;
    }
}
