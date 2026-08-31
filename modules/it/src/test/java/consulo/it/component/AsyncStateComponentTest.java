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
package consulo.it.component;

import consulo.application.Application;
import consulo.component.ProviderAsync;
import consulo.component.internal.inject.InjectingContainer;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.component.persist.PersistentStateComponentAsync;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.it.HeadlessApplicationExtension;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises asynchronous service creation against a real application, which the unit tests in component-impl
 * cannot do - they have no {@code UIAccess} and no coroutine context.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class AsyncStateComponentTest {
    @Test
    public void getAsyncCreatesInstanceOffTheUiThread(Application application) throws Exception {
        AsyncService.ourCreated.set(0);
        InjectingContainer container = childContainer(application);

        NeedsAsyncService holder = container.getInstance(NeedsAsyncService.class);
        assertThat(AsyncService.ourCreated).hasValue(0);

        AsyncService service = holder.myProvider.getAsync().get(30, TimeUnit.SECONDS);

        assertThat(service).isNotNull();
        assertThat(AsyncService.ourCreated).hasValue(1);
        assertThat(service.myCreatedOnUiThread).isFalse();
    }

    @Test
    public void getAsyncReturnsCompletedFutureOnceCreated(Application application) throws Exception {
        AsyncService.ourCreated.set(0);
        InjectingContainer container = childContainer(application);

        NeedsAsyncService holder = container.getInstance(NeedsAsyncService.class);
        AsyncService first = holder.myProvider.getAsync().get(30, TimeUnit.SECONDS);

        CompletableFuture<AsyncService> second = holder.myProvider.getAsync();

        assertThat(second).isCompleted();
        assertThat(second.get()).isSameAs(first);
        assertThat(AsyncService.ourCreated).hasValue(1);
    }

    @Test
    public void concurrentGetAsyncSharesOneInstantiation(Application application) throws Exception {
        AsyncService.ourCreated.set(0);
        InjectingContainer container = childContainer(application);

        NeedsAsyncService holder = container.getInstance(NeedsAsyncService.class);

        List<CompletableFuture<AsyncService>> futures = List.of(
            holder.myProvider.getAsync(),
            holder.myProvider.getAsync(),
            holder.myProvider.getAsync(),
            holder.myProvider.getAsync());

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(30, TimeUnit.SECONDS);

        AsyncService first = futures.get(0).get();
        for (CompletableFuture<AsyncService> future : futures) {
            assertThat(future.get()).isSameAs(first);
        }
        assertThat(AsyncService.ourCreated).hasValue(1);
    }

    @Test
    public void getIsRejectedOnTheUiThread(Application application) throws Exception {
        AsyncService.ourCreated.set(0);
        InjectingContainer container = childContainer(application);

        NeedsAsyncService holder = container.getInstance(NeedsAsyncService.class);

        CompletableFuture<Throwable> failure = new CompletableFuture<>();
        CompletableFuture<Boolean> ranOnUiThread = new CompletableFuture<>();
        application.getLastUIAccess().give(() -> {
            ranOnUiThread.complete(UIAccess.isUIThread());
            try {
                holder.myProvider.get();
                failure.complete(null);
            }
            catch (Throwable t) {
                failure.complete(t);
            }
        });

        // guards against the assertion below passing because nothing is ever seen as the UI thread here
        assertThat(ranOnUiThread.get(30, TimeUnit.SECONDS)).isTrue();
        assertThat(failure.get(30, TimeUnit.SECONDS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("getAsync()");
        assertThat(AsyncService.ourCreated).hasValue(0);
    }

    @Test
    public void directInjectionIsRejected(Application application) {
        InjectingContainerBuilder builder = application.getInjectingContainer().childBuilder();
        builder.bind(AsyncService.class).forceSingleton();
        builder.bind(NeedsAsyncServiceDirectly.class).forceSingleton();
        InjectingContainer container = builder.build();

        assertThatThrownBy(() -> container.getInstance(NeedsAsyncServiceDirectly.class))
            .hasMessageContaining(AsyncService.class.getName())
            .hasMessageContaining("ProviderAsync");
    }

    private static InjectingContainer childContainer(Application application) {
        InjectingContainerBuilder builder = application.getInjectingContainer().childBuilder();
        builder.bind(AsyncService.class).forceSingleton();
        builder.bind(NeedsAsyncService.class).forceSingleton();
        return builder.build();
    }

    @State(name = "AsyncStateComponentTestService", storages = @Storage("async-state-component-test.xml"))
    public static class AsyncService implements PersistentStateComponentAsync<AsyncService.Bean> {
        public static final AtomicInteger ourCreated = new AtomicInteger();

        public final boolean myCreatedOnUiThread;

        @Inject
        public AsyncService() {
            ourCreated.incrementAndGet();
            myCreatedOnUiThread = UIAccess.isUIThread();
        }

        @Override
        public Coroutine<?, Bean> getState() {
            return Coroutine.first(CodeExecution.apply(input -> new Bean()));
        }

        @Override
        public Coroutine<?, ?> loadState(Bean state) {
            return Coroutine.empty();
        }

        public static class Bean {
            public String name = "default";
        }
    }

    public static class NeedsAsyncService {
        public final ProviderAsync<AsyncService> myProvider;

        @Inject
        public NeedsAsyncService(ProviderAsync<AsyncService> provider) {
            myProvider = provider;
        }
    }

    public static class NeedsAsyncServiceDirectly {
        @Inject
        public NeedsAsyncServiceDirectly(AsyncService service) {
        }
    }
}
