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
package consulo.ui.ex.action;

import consulo.util.dataholder.Key;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Gives an update access to the state of OTHER actions computed within the same expansion
 * (menu, toolbar or popup update tick), instead of calling their update logic directly.
 * <p>
 * The results are cached per expansion: asking for the same action or group from several
 * places computes it only once.
 * <p>
 * The asynchronous queries are meant to be awaited from {@link AnActionWithAsyncUpdate}
 * update chains. A plain {@link AnActionWithSyncUpdate} may only use {@link #sharedData},
 * since it cannot await a future.
 * <p>
 * Available via {@link AnActionEvent#getUpdateSession()} during an expansion; outside of it
 * (e.g. a single-action update from the keyboard dispatcher) the event carries {@link #EMPTY}.
 */
public interface ActionUpdateSession {
    ActionUpdateSession EMPTY = new ActionUpdateSession() {
        @Override
        public CompletableFuture<Presentation> presentation(AnAction action) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("no update session in this context"));
        }

        @Override
        public CompletableFuture<List<AnAction>> children(ActionGroup group) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("no update session in this context"));
        }

        @Override
        public <T> T sharedData(Key<T> key, Supplier<? extends T> provider) {
            return provider.get();
        }
    };

    /**
     * The updated presentation of another action, computed at most once per expansion.
     */
    CompletableFuture<Presentation> presentation(AnAction action);

    /**
     * The children of a group, computed at most once per expansion.
     */
    CompletableFuture<List<AnAction>> children(ActionGroup group);

    /**
     * Computes a value at most once per expansion and shares it between all actions of the expansion.
     */
    <T> T sharedData(Key<T> key, Supplier<? extends T> provider);
}
