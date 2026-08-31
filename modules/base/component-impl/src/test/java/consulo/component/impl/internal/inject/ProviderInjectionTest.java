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
package consulo.component.impl.internal.inject;

import consulo.component.ProviderAsync;
import consulo.component.internal.inject.InjectingContainer;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.component.persist.PersistentStateComponentAsync;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.concurrent.coroutine.Coroutine;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProviderInjectionTest {
    private InjectingContainerBuilder myBuilder;

    @BeforeEach
    public void setUp() {
        Leaf.ourCreated.set(0);
        NeedsProvider.ourCreated.set(0);
        myBuilder = new DefaultInjectingContainerBuilder(null);
    }

    @Test
    public void directParameterIsInjected() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(NeedsDirect.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        NeedsDirect instance = container.getInstance(NeedsDirect.class);

        assertThat(instance.myLeaf).isSameAs(container.getInstance(Leaf.class));
        assertThat(Leaf.ourCreated).hasValue(1);
    }

    @Test
    public void providerParameterIsInjectedAndResolvesToSameSingleton() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(NeedsProvider.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        NeedsProvider instance = container.getInstance(NeedsProvider.class);

        assertThat(instance.myLeafProvider.get()).isSameAs(container.getInstance(Leaf.class));
    }

    @Test
    public void providerParameterDefersTargetCreation() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(NeedsProvider.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        NeedsProvider instance = container.getInstance(NeedsProvider.class);
        assertThat(Leaf.ourCreated).hasValue(0);

        instance.myLeafProvider.get();
        assertThat(Leaf.ourCreated).hasValue(1);
    }

    @Test
    public void providerCachesResolvedInstance() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(NeedsProvider.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        NeedsProvider instance = container.getInstance(NeedsProvider.class);

        assertThat(instance.myLeafProvider.get()).isSameAs(instance.myLeafProvider.get());
        assertThat(Leaf.ourCreated).hasValue(1);
    }

    @Test
    public void singletonIsCreatedOnce() {
        myBuilder.bind(Leaf.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        assertThat(container.getInstance(Leaf.class)).isSameAs(container.getInstance(Leaf.class));
        assertThat(Leaf.ourCreated).hasValue(1);
    }

    /**
     * ConstructorInjectionComponentAdapter#getGreediestSatisfiableConstructor is the PicoContainer
     * algorithm, which requires its input sorted by descending parameter count. #getConstructors
     * returns Class#getDeclaredConstructors unsorted, so the first satisfiable constructor in
     * declaration order wins rather than the widest one.
     */
    @Test
    public void firstSatisfiableConstructorInDeclarationOrderIsChosen() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(Other.class).forceSingleton();
        myBuilder.bind(NarrowFirst.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        NarrowFirst instance = container.getInstance(NarrowFirst.class);

        assertThat(instance.myUsedArgumentCount).isEqualTo(1);
    }

    @Test
    public void widerConstructorIsChosenOnlyWhenDeclaredFirst() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(Other.class).forceSingleton();
        myBuilder.bind(WideFirst.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        WideFirst instance = container.getInstance(WideFirst.class);

        assertThat(instance.myUsedArgumentCount).isEqualTo(2);
    }

    @Test
    public void unsatisfiableConstructorIsSkipped() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(WideFirst.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        WideFirst instance = container.getInstance(WideFirst.class);

        assertThat(instance.myUsedArgumentCount).isEqualTo(1);
    }

    /**
     * The first {@link Inject} annotated constructor short circuits the greedy search entirely -
     * see ConstructorInjectionComponentAdapter#getGreediestSatisfiableConstructor. Greediness only
     * applies when no constructor carries the annotation.
     */
    @Test
    public void injectAnnotationWinsOverWiderSatisfiableConstructor() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(Other.class).forceSingleton();
        myBuilder.bind(NarrowInject.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        NarrowInject instance = container.getInstance(NarrowInject.class);

        assertThat(instance.myUsedArgumentCount).isEqualTo(1);
    }

    @Test
    public void providerAsyncParameterIsInjected() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(NeedsProviderAsync.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        NeedsProviderAsync instance = container.getInstance(NeedsProviderAsync.class);

        assertThat(instance.myLeafProvider.getAsync().join()).isSameAs(container.getInstance(Leaf.class));
    }

    @Test
    public void plainProviderIsAlsoProviderAsync() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(NeedsProvider.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        NeedsProvider instance = container.getInstance(NeedsProvider.class);

        assertThat(instance.myLeafProvider).isInstanceOf(ProviderAsync.class);
    }

    @Test
    public void getAsyncReturnsCompletedFutureForExistingSingleton() {
        myBuilder.bind(Leaf.class).forceSingleton();
        myBuilder.bind(NeedsProviderAsync.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        NeedsProviderAsync instance = container.getInstance(NeedsProviderAsync.class);
        Leaf created = container.getInstance(Leaf.class);

        CompletableFuture<Leaf> future = instance.myLeafProvider.getAsync();

        assertThat(future).isCompletedWithValue(created);
        assertThat(Leaf.ourCreated).hasValue(1);
    }

    @Test
    public void asyncStateComponentCanNotBeInjectedDirectly() {
        myBuilder.bind(AsyncStateService.class).forceSingleton();
        myBuilder.bind(NeedsAsyncStateDirectly.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        assertThatThrownBy(() -> container.getInstance(NeedsAsyncStateDirectly.class))
            .isInstanceOf(AsyncInjectionNotSupportedException.class)
            .hasMessageContaining(AsyncStateService.class.getName())
            .hasMessageContaining("ProviderAsync");
    }

    @Test
    public void asyncStateComponentCanBeInjectedAsProvider() {
        myBuilder.bind(AsyncStateService.class).forceSingleton();
        myBuilder.bind(NeedsAsyncStateProvider.class).forceSingleton();
        InjectingContainer container = myBuilder.build();

        NeedsAsyncStateProvider instance = container.getInstance(NeedsAsyncStateProvider.class);

        assertThat(instance.myProvider).isNotNull();
    }

    @Test
    public void childContainerResolvesParentBinding() {
        myBuilder.bind(Leaf.class).forceSingleton();
        InjectingContainer parent = myBuilder.build();

        InjectingContainerBuilder childBuilder = parent.childBuilder();
        childBuilder.bind(NeedsDirect.class).forceSingleton();
        InjectingContainer child = childBuilder.build();

        NeedsDirect instance = child.getInstance(NeedsDirect.class);

        assertThat(instance.myLeaf).isSameAs(parent.getInstance(Leaf.class));
        assertThat(Leaf.ourCreated).hasValue(1);
    }

    public static class Leaf {
        public static final AtomicInteger ourCreated = new AtomicInteger();

        @Inject
        public Leaf() {
            ourCreated.incrementAndGet();
        }
    }

    public static class Other {
    }

    public static class NeedsDirect {
        public final Leaf myLeaf;

        @Inject
        public NeedsDirect(Leaf leaf) {
            myLeaf = leaf;
        }
    }

    public static class NeedsProvider {
        public static final AtomicInteger ourCreated = new AtomicInteger();

        public final Provider<Leaf> myLeafProvider;

        @Inject
        public NeedsProvider(Provider<Leaf> leafProvider) {
            myLeafProvider = leafProvider;
            ourCreated.incrementAndGet();
        }
    }

    public static class NeedsProviderAsync {
        public final ProviderAsync<Leaf> myLeafProvider;

        @Inject
        public NeedsProviderAsync(ProviderAsync<Leaf> leafProvider) {
            myLeafProvider = leafProvider;
        }
    }

    @State(name = "ProviderInjectionTestAsyncState", storages = @Storage("provider-injection-test.xml"))
    public static class AsyncStateService implements PersistentStateComponentAsync<String> {
        @Inject
        public AsyncStateService() {
        }

        @Override
        public Coroutine<?, String> getState() {
            return Coroutine.empty();
        }

        @Override
        public Coroutine<?, ?> loadState(String state) {
            return Coroutine.empty();
        }
    }

    public static class NeedsAsyncStateDirectly {
        @Inject
        public NeedsAsyncStateDirectly(AsyncStateService service) {
        }
    }

    public static class NeedsAsyncStateProvider {
        public final Provider<AsyncStateService> myProvider;

        @Inject
        public NeedsAsyncStateProvider(Provider<AsyncStateService> provider) {
            myProvider = provider;
        }
    }

    public static class NarrowFirst {
        public final int myUsedArgumentCount;

        public NarrowFirst(Leaf leaf) {
            myUsedArgumentCount = 1;
        }

        public NarrowFirst(Leaf leaf, Other other) {
            myUsedArgumentCount = 2;
        }
    }

    public static class WideFirst {
        public final int myUsedArgumentCount;

        public WideFirst(Leaf leaf, Other other) {
            myUsedArgumentCount = 2;
        }

        public WideFirst(Leaf leaf) {
            myUsedArgumentCount = 1;
        }
    }

    public static class NarrowInject {
        public final int myUsedArgumentCount;

        @Inject
        public NarrowInject(Leaf leaf) {
            myUsedArgumentCount = 1;
        }

        public NarrowInject(Leaf leaf, Other other) {
            myUsedArgumentCount = 2;
        }
    }
}
