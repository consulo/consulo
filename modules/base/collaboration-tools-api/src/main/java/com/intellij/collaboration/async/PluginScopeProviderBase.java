// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import consulo.disposer.Disposable;
import consulo.ui.ex.awt.DialogWrapper;
import kotlinx.coroutines.CoroutineScope;
import jakarta.annotation.Nonnull;

import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

import java.util.function.Function;

/**
 * A base class for a plugin service which can supply a coroutine scope to places where structured concurrency is not possible.
 * Can be subclassed and registered as a light service.
 */
@ApiStatus.Internal
public class PluginScopeProviderBase {
    private final @Nonnull CoroutineScope parentCs;

    public PluginScopeProviderBase(@Nonnull CoroutineScope parentCs) {
        this.parentCs = parentCs;
    }

    public @Nonnull CoroutineScope createDisposedScope(
        @Nonnull String name,
        @Nonnull Disposable disposable,
        @Nonnull CoroutineContext context
    ) {
        CoroutineScope scope = ChildScopeKt.childScope(parentCs, name, context);
        CoroutineUtil.cancelledWith(scope, disposable);
        return scope;
    }

    public @Nonnull CoroutineScope createDisposedScope(@Nonnull String name, @Nonnull Disposable disposable) {
        return createDisposedScope(name, disposable, EmptyCoroutineContext.INSTANCE);
    }

    public <D extends DialogWrapper> @Nonnull D constructDialog(
        @Nonnull String name,
        @Nonnull Function<CoroutineScope, D> constructor
    ) {
        CoroutineScope cs = ChildScopeKt.childScope(parentCs, name);
        D dialog = constructor.apply(cs);
        CoroutineUtil.cancelledWith(cs, dialog.getDisposable());
        return dialog;
    }
}
