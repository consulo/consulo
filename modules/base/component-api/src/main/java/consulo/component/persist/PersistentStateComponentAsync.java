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
package consulo.component.persist;

import consulo.component.internal.StateComponent;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

/**
 * A state component whose state is both read and written asynchronously. Every step is a {@link Coroutine},
 * so a component may hop to the UI thread (via a {@code UIAction} step) for UI-bound work while the rest of
 * the save or load runs off the EDT.
 * <p>
 * Loading such a component requires running a coroutine, which cannot be done on the UI thread. A service
 * implementing this interface therefore must not be injected as a plain constructor parameter - inject
 * {@link jakarta.inject.Provider} or {@link consulo.component.ProviderAsync} instead.
 *
 * @author VISTALL
 * @since 2026-07-29
 */
public non-sealed interface PersistentStateComponentAsync<T> extends StateComponent {
    /**
     * Returned by {@link #getStateModificationCount()} when the component does not track modifications,
     * in which case its state is serialized on every save.
     */
    long UNTRACKED = -1;

    /**
     * @return a coroutine producing the component state, or producing {@code null} when nothing should be stored
     */
    Coroutine<?, @Nullable T> getState();

    Coroutine<?, ?> loadState(T state);

    /**
     * Runs after {@link #loadState(Object)}, even when no state existed. Also runs on every reload, when the
     * state changed on disk or was synchronized from external storage.
     *
     * @param first true on the initial load right after the component was created, false on a reload
     */
    default Coroutine<?, ?> afterLoad(boolean first) {
        return Coroutine.empty();
    }

    /**
     * Implement to let the store skip serializing this component while it is unchanged.
     */
    default long getStateModificationCount() {
        return UNTRACKED;
    }
}
