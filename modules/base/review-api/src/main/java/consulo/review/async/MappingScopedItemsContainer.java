// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.async;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.review.util.HashingUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Allows mapping a collection of items {@code T} to scoped ({@link Disposable} bound) values {@code V}
 */
public final class MappingScopedItemsContainer<T, V> {
    private final Disposable myParentDisposable;
    private final HashingStrategy<T> myHashingStrategy;
    private final BiFunction<Disposable, T, V> myMapper;
    private final @Nullable BiConsumer<V, T> myUpdate;
    private final MutableObservableValue<List<V>> myMappedState = MutableObservableValue.of(List.of());

    private Map<T, ScopedValue<V>> myMapping;

    /**
     * @param parentDisposable parent for value scopes
     * @param hashingStrategy  strategy used to compare items
     * @param mapper           factory function to create a value from item
     * @param update           function used to update value if a new item is supplied for the existing one
     */
    public MappingScopedItemsContainer(Disposable parentDisposable,
                                HashingStrategy<T> hashingStrategy,
                                BiFunction<Disposable, T, V> mapper,
                                @Nullable BiConsumer<V, T> update) {
        myParentDisposable = parentDisposable;
        myHashingStrategy = hashingStrategy;
        myMapper = mapper;
        myUpdate = update;
        myMapping = Maps.newLinkedHashMap(hashingStrategy);
    }

    public static <T, V> MappingScopedItemsContainer<T, V> byIdentity(Disposable parentDisposable, BiFunction<Disposable, T, V> mapper) {
        return new MappingScopedItemsContainer<>(parentDisposable, HashingStrategy.identity(), mapper, (value, item) -> {
        });
    }

    public static <T, V> MappingScopedItemsContainer<T, V> byEquality(Disposable parentDisposable, BiFunction<Disposable, T, V> mapper) {
        return new MappingScopedItemsContainer<>(parentDisposable, HashingStrategy.canonical(), mapper, (value, item) -> {
        });
    }

    public ObservableValue<List<V>> getMappedState() {
        return myMappedState;
    }

    public void update(Iterable<T> items) {
        synchronized (this) {
            Map<T, ScopedValue<V>> currentMap = myMapping;
            Map<T, ScopedValue<V>> resultMap = Maps.newLinkedHashMap(myHashingStrategy);
            // add everything to the new mapping
            for (T item : items) {
                ScopedValue<V> existing = currentMap.get(item);
                if (existing == null) {
                    Disposable valueScope = Disposable.newDisposable("item=" + item);
                    Disposer.register(myParentDisposable, valueScope);
                    resultMap.put(item, new ScopedValue<>(valueScope, myMapper.apply(valueScope, item)));
                }
                else {
                    if (myUpdate != null) {
                        myUpdate.accept(existing.value(), item);
                    }
                    resultMap.put(item, existing);
                }
            }
            // cancel scopes that are no longer included in the map
            for (Map.Entry<T, ScopedValue<V>> entry : currentMap.entrySet()) {
                if (!resultMap.containsKey(entry.getKey())) {
                    Disposer.dispose(entry.getValue().scope());
                }
            }
            myMapping = resultMap;
        }
        publish();
    }

    public V addIfAbsent(T item) {
        V value;
        synchronized (this) {
            ScopedValue<V> existing = myMapping.get(item);
            if (existing != null) {
                return existing.value();
            }
            Map<T, ScopedValue<V>> newMapping = Maps.newLinkedHashMap(myHashingStrategy);
            newMapping.putAll(myMapping);
            Disposable valueScope = Disposable.newDisposable("item=" + item);
            Disposer.register(myParentDisposable, valueScope);
            value = myMapper.apply(valueScope, item);
            newMapping.put(item, new ScopedValue<>(valueScope, value));
            myMapping = newMapping;
        }
        publish();
        return value;
    }

    private void publish() {
        List<V> values = new ArrayList<>();
        synchronized (this) {
            for (ScopedValue<V> scopedValue : myMapping.values()) {
                values.add(scopedValue.value());
            }
        }
        myMappedState.set(List.copyOf(values));
    }

    /**
     * Creates a list of stateful objects from the list of DTO (Data Transfer Object)
     * Stateful objects are updated with {@code update} when a DTO identified by a {@code sourceIdentifier} changes
     */
    public static <T, R> ObservableValue<List<R>> mapDataToModel(Disposable parentDisposable,
                                                                 ObservableValue<? extends Iterable<T>> source,
                                                                 Function<T, ?> sourceIdentifier,
                                                                 BiFunction<Disposable, T, R> mapper,
                                                                 BiConsumer<R, T> update) {
        return mapCaching(parentDisposable, source, HashingUtil.mappingStrategy(sourceIdentifier), mapper, update);
    }

    /**
     * Creates a list of stateful objects from other stateful objects, comparing the original objects by identity
     */
    public static <T, R> ObservableValue<List<R>> mapStatefulToStateful(Disposable parentDisposable,
                                                                        ObservableValue<? extends Iterable<T>> source,
                                                                        BiFunction<Disposable, T, R> mapper) {
        return mapCaching(parentDisposable, source, HashingStrategy.identity(), mapper, null);
    }

    /**
     * Maps each <i>item</i> {@code T} in the iterable of {@code source} to a <i>value</i> {@code V}
     * <p>
     * Items are distinguished by a {@code hashingStrategy}
     * <p>
     * When a new iterable is received:
     * <ul>
     * <li>a new {@link Disposable} and a new value is created via {@code mapper} for new items</li>
     * <li>existing values are updated via {@code update} if it was supplied</li>
     * <li>values for missing items are removed and their scope is disposed</li>
     * </ul>
     * Order of the values in the resulting list is the same as in the source iterable
     */
    private static <T, V> ObservableValue<List<V>> mapCaching(Disposable parentDisposable,
                                                              ObservableValue<? extends Iterable<T>> source,
                                                              HashingStrategy<T> hashingStrategy,
                                                              BiFunction<Disposable, T, V> mapper,
                                                              @Nullable BiConsumer<V, T> update) {
        MappingScopedItemsContainer<T, V> container = new MappingScopedItemsContainer<>(parentDisposable, hashingStrategy, mapper, update);
        Runnable unsubscribe = source.addListener(container::update);
        Disposer.register(parentDisposable, unsubscribe::run);
        container.update(source.get());
        return container.getMappedState();
    }
}
