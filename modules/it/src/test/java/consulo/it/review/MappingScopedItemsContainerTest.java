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
package consulo.it.review;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.review.async.MappingScopedItemsContainer;
import consulo.review.util.HashingUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MappingScopedItemsContainerTest {
    private final Disposable myDisposable = Disposable.newDisposable();

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myDisposable);
    }

    @Test
    public void customHashingKeepsReusedScopesActive() {
        HashingStrategy<KeyValueItem> keyMappingHashingStrategy = HashingUtil.mappingStrategy(KeyValueItem::key);
        MappingScopedItemsContainer<KeyValueItem, ScopedItem> container =
            new MappingScopedItemsContainer<>(myDisposable, keyMappingHashingStrategy, ScopedItem::new, (value, item) -> {
            });
        KeyValueItem first = new KeyValueItem(1, "a");
        KeyValueItem second = new KeyValueItem(1, "b");

        // make sure that the test items are NOT equal, but if we use a custom hashing strategy, they are equal
        assertThat(first).isNotEqualTo(second);
        assertThat(keyMappingHashingStrategy.equals(first, second)).isTrue();

        container.update(List.of(first));
        ScopedItem firstMapped = container.getMappedState().get().get(0);

        container.update(List.of(second));
        ScopedItem secondMapped = container.getMappedState().get().get(0);

        assertThat(secondMapped.myItem).isNotSameAs(second);
        assertThat(secondMapped.myItem).isSameAs(first);
        assertThat(secondMapped.myScope).isSameAs(firstMapped.myScope);
        assertThat(Disposer.isDisposed(secondMapped.myScope)).isFalse();
    }

    @Test
    public void updateFunctionIsAppliedToTheValueAndItsScopeStaysTheSame() {
        HashingStrategy<KeyValueItem> keyMappingHashingStrategy = HashingUtil.mappingStrategy(KeyValueItem::key);
        MappingScopedItemsContainer<KeyValueItem, ScopedItem> container =
            new MappingScopedItemsContainer<>(myDisposable, keyMappingHashingStrategy, ScopedItem::new, (value, item) -> value.myItem = item);
        // items are equal by the custom hashing strategy
        KeyValueItem first = new KeyValueItem(1, "a");
        KeyValueItem second = new KeyValueItem(1, "b");

        container.update(List.of(first));
        ScopedItem firstMapped = container.getMappedState().get().get(0);

        container.update(List.of(second));
        ScopedItem secondMapped = container.getMappedState().get().get(0);

        assertThat(secondMapped.myItem).isNotSameAs(first);
        assertThat(secondMapped.myItem).isSameAs(second);
        assertThat(secondMapped.myScope).isSameAs(firstMapped.myScope);
        assertThat(Disposer.isDisposed(secondMapped.myScope)).isFalse();
    }

    @Test
    public void mapStatefulToStatefulDisposesDeletedScopes() {
        MutableObservableValue<List<Integer>> input = MutableObservableValue.of(List.of());
        ObservableValue<List<ScopedItem>> output =
            MappingScopedItemsContainer.mapStatefulToStateful(myDisposable, input, (scope, item) -> new ScopedItem(scope, new KeyValueItem(item, "")));
        assertThat(output.get()).isEmpty();

        // initial add
        input.set(List.of(1));
        ScopedItem emittedItem1 = output.get().get(0);
        assertThat(emittedItem1.myItem.key()).isEqualTo(1);

        // add another
        input.set(List.of(1, 2));
        assertThat(output.get()).hasSize(2);
        assertThat(output.get().get(0)).isSameAs(emittedItem1);

        // remove the first one
        input.set(List.of(2));
        assertThat(output.get()).hasSize(1);
        assertThat(Disposer.isDisposed(emittedItem1.myScope)).isTrue();
        assertThat(Disposer.isDisposed(output.get().get(0).myScope)).isFalse();
    }

    private record KeyValueItem(int key, String value) {
    }

    private static final class ScopedItem {
        private final Disposable myScope;
        private KeyValueItem myItem;

        ScopedItem(Disposable scope, KeyValueItem item) {
            myScope = scope;
            myItem = item;
        }
    }
}
