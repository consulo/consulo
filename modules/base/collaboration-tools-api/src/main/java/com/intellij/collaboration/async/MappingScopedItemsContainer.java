// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.impl.CollectionFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Allows mapping a collection of items {@code T} to scoped (coroutine scope bound) values {@code V}.
 * An intermittent key {@code K} is used to uniquely identify items.
 *
 * @param <T> item type
 * @param <K> key type
 * @param <V> value type
 */
@ApiStatus.Internal
public final class MappingScopedItemsContainer<T, K, V> {
    private final @Nonnull CoroutineScope cs;
    private final @Nonnull Function<T, K> keyExtractor;
    private final @Nonnull HashingStrategy<K> hashingStrategy;
    private final @Nonnull BiFunction<CoroutineScope, T, V> mapper;
    private final @Nullable BiConsumer<V, T> update;

    private final @Nonnull MutableStateFlow<Map<K, ScopingWrapper<V>>> _mappingState =
        StateFlowKt.MutableStateFlow(Map.of());

    private final @Nonnull StateFlow<Map<K, V>> mappingState;
    private final @Nonnull Object mapGuard = new Object();

    public MappingScopedItemsContainer(
        @Nonnull CoroutineScope cs,
        @Nonnull Function<T, K> keyExtractor,
        @Nonnull HashingStrategy<K> hashingStrategy,
        @Nonnull BiFunction<CoroutineScope, T, V> mapper,
        @Nullable BiConsumer<V, T> update
    ) {
        this.cs = cs;
        this.keyExtractor = keyExtractor;
        this.hashingStrategy = hashingStrategy;
        this.mapper = mapper;
        this.update = update;
        this.mappingState = CoroutineUtil.mapState(_mappingState, map -> {
            Map<K, V> result = CollectionFactory.createLinkedCustomHashingStrategyMap(hashingStrategy);
            for (Map.Entry<K, ScopingWrapper<V>> entry : map.entrySet()) {
                result.put(entry.getKey(), entry.getValue().value);
            }
            return result;
        });
    }

    public @Nonnull StateFlow<Map<K, V>> getMappingState() {
        return mappingState;
    }

    @SuppressWarnings("unchecked")
    public void update(@Nonnull Iterable<T> items) {
        synchronized (mapGuard) {
            Map<K, ScopingWrapper<V>> currentMap = (Map<K, ScopingWrapper<V>>) _mappingState.getValue();
            Map<K, ScopingWrapper<V>> resultMap = CollectionFactory.createLinkedCustomHashingStrategyMap(hashingStrategy);

            for (T item : items) {
                K itemKey = keyExtractor.apply(item);
                ScopingWrapper<V> existing = currentMap.get(itemKey);

                if (existing == null) {
                    CoroutineScope valueScope = ChildScopeKt.childScope(cs, "item=" + itemKey);
                    resultMap.put(itemKey, new ScopingWrapper<>(valueScope, mapper.apply(valueScope, item)));
                }
                else {
                    if (update != null) {
                        update.accept(existing.value, item);
                    }
                    resultMap.put(itemKey, existing);
                }
            }

            // cancel scopes that are no longer included in the list
            Set<K> currentKeys = currentMap.keySet();
            for (K key : currentKeys) {
                if (!resultMap.containsKey(key)) {
                    ScopingWrapper<V> scopedValue = currentMap.get(key);
                    if (scopedValue != null) {
                        kotlinx.coroutines.CoroutineScopeKt.cancel(scopedValue.scope, null);
                    }
                }
            }

            if (currentMap.size() != resultMap.size() || !currentKeys.equals(resultMap.keySet())) {
                _mappingState.setValue(resultMap);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public @Nonnull V addIfAbsent(@Nonnull T item) {
        synchronized (mapGuard) {
            K key = keyExtractor.apply(item);
            Map<K, ScopingWrapper<V>> current = (Map<K, ScopingWrapper<V>>) _mappingState.getValue();
            ScopingWrapper<V> existing = current.get(key);
            if (existing != null) {
                return existing.value;
            }

            CoroutineScope valueScope = ChildScopeKt.childScope(cs, "item=" + key);
            ScopingWrapper<V> newValue = new ScopingWrapper<>(valueScope, mapper.apply(valueScope, item));
            Map<K, ScopingWrapper<V>> updated = CollectionFactory.createLinkedCustomHashingStrategyMap(hashingStrategy);
            updated.putAll(current);
            updated.put(key, newValue);
            _mappingState.setValue(updated);
            return newValue.value;
        }
    }

    public static <T, V> @Nonnull MappingScopedItemsContainer<T, T, V> byIdentity(
        @Nonnull CoroutineScope cs,
        @Nonnull BiFunction<CoroutineScope, T, V> mapper
    ) {
        return new MappingScopedItemsContainer<>(cs, Function.identity(), HashingStrategy.identity(), mapper, (v, t) -> {
        });
    }

    public static <T, V> @Nonnull MappingScopedItemsContainer<T, T, V> byEquality(
        @Nonnull CoroutineScope cs,
        @Nonnull BiFunction<CoroutineScope, T, V> mapper
    ) {
        return new MappingScopedItemsContainer<>(cs, Function.identity(), HashingStrategy.canonical(), mapper, (v, t) -> {
        });
    }

    private record ScopingWrapper<T>(@Nonnull CoroutineScope scope, @Nonnull T value) {
    }
}
