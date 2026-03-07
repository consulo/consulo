// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui;

import consulo.proxy.EventDispatcher;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;
import java.util.function.Function;

public final class SingleValueModel<T> {
    private final EventDispatcher<SimpleEventListener> changeEventDispatcher = EventDispatcher.create(SimpleEventListener.class);
    private T value;

    public SingleValueModel(T initialValue) {
        this.value = initialValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        changeEventDispatcher.getMulticaster().eventOccurred();
    }

    @RequiresEdt
    public void addAndInvokeListener(@Nonnull Consumer<T> listener) {
        addListener(listener);
        listener.accept(value);
    }

    @RequiresEdt
    public void addListener(@Nonnull Consumer<T> listener) {
        SimpleEventListener.addListener(changeEventDispatcher, () -> listener.accept(value));
    }

    public <R> @Nonnull SingleValueModel<R> map(@Nonnull Function<T, R> mapper) {
        SingleValueModel<R> mappedModel = new SingleValueModel<>(mapper.apply(value));
        this.addListener(v -> mappedModel.setValue(mapper.apply(value)));
        return mappedModel;
    }
}
