// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import jakarta.annotation.Nonnull;

import java.util.function.UnaryOperator;

@ApiStatus.Experimental
public abstract class CollectableSerializablePersistentStateComponent<T> extends SerializablePersistentStateComponent<T> {
    protected final MutableStateFlow<T> stateFlow;

    protected CollectableSerializablePersistentStateComponent(@Nonnull T defaultState) {
        super(defaultState);
        this.stateFlow = StateFlowKt.MutableStateFlow(defaultState);
    }

    @Override
    public void loadState(@Nonnull T state) {
        super.loadState(state);
        stateFlow.setValue(state);
    }

    protected void updateStateAndEmit(@Nonnull UnaryOperator<T> updateFunction) {
        updateState(updateFunction::apply);
        stateFlow.setValue(getState());
    }
}
