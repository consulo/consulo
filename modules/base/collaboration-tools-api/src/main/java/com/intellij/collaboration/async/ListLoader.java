// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import kotlinx.coroutines.flow.FlowKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents some list of data we need to fetch and maintain to show in UI.
 */
@ApiStatus.Internal
public interface ListLoader<V> {

    @Nonnull
    StateFlow<State<V>> getStateFlow();

    @Nonnull
    StateFlow<Boolean> getIsBusyFlow();

    final class State<V> {
        private final @Nullable List<V> list;
        private final @Nullable Throwable error;

        public State() {
            this(null, null);
        }

        public State(@Nullable List<V> list, @Nullable Throwable error) {
            this.list = list;
            this.error = error;
        }

        public @Nullable List<V> getList() {
            return list;
        }

        public @Nullable Throwable getError() {
            return error;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof State<?> state)) {
                return false;
            }
            return Objects.equals(list, state.list) && Objects.equals(error, state.error);
        }

        @Override
        public int hashCode() {
            return Objects.hash(list, error);
        }

        @Override
        public String toString() {
            return "State(list=" + list + ", error=" + error + ")";
        }
    }
}
