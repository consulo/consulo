// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import kotlinx.coroutines.flow.FlowKt;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract mutable list loader that supports client-side updates.
 */
@ApiStatus.Internal
public abstract class MutableListLoader<V> implements ListLoader<V> {
    protected final @Nonnull MutableStateFlow<State<V>> mutableStateFlow = StateFlowKt.MutableStateFlow(new State<>());

    @Override
    public @Nonnull StateFlow<State<V>> getStateFlow() {
        return FlowKt.asStateFlow(mutableStateFlow);
    }

    /**
     * Manually updates the list and cancels all running refreshes
     * so that changes to the list are not immediately overwritten
     * by scheduled refreshes.
     *
     * @param change An object that represents a change to be applied.
     */
    public void update(@Nonnull Change<V> change) {
        while (true) {
            State<V> current = (State<V>) mutableStateFlow.getValue();
            List<V> newList;
            if (change instanceof AddedFirst<V> addedFirst) {
                List<V> result = new ArrayList<>();
                result.add(addedFirst.getValue());
                if (current.getList() != null) {
                    result.addAll(current.getList());
                }
                newList = result;
            }
            else if (change instanceof AddedLast<V> addedLast) {
                List<V> result = new ArrayList<>(current.getList() != null ? current.getList() : List.of());
                result.add(addedLast.getValue());
                newList = result;
            }
            else if (change instanceof AddedAllLast<V> addedAllLast) {
                List<V> result = new ArrayList<>(current.getList() != null ? current.getList() : List.of());
                result.addAll(addedAllLast.getValues());
                newList = result;
            }
            else if (change instanceof Deleted<V> deleted) {
                newList = current.getList() != null
                    ? current.getList().stream().filter(v -> !deleted.getIsDeleted().test(v)).collect(Collectors.toList())
                    : null;
            }
            else if (change instanceof Updated<V> updated) {
                newList = current.getList() != null
                    ? current.getList().stream().map(updated.getUpdater()).collect(Collectors.toList())
                    : null;
            }
            else {
                throw new IllegalArgumentException("Unknown change type: " + change);
            }
            State<V> newState = new State<>(newList, current.getError());
            if (mutableStateFlow.compareAndSet(current, newState)) {
                break;
            }
        }
    }
}
