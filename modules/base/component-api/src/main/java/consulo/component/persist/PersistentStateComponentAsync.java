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

import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

/**
 * A {@link PersistentStateComponent} whose state must be computed on a specific thread (typically the UI thread).
 * Instead of the synchronous {@link #getState()} the store fetches the state through {@link #getStateAsync()},
 * a coroutine that can hop to the UI thread (via a {@code UIAction} step) for UI-bound reads while the rest of
 * the save runs off the EDT.
 *
 * @author VISTALL
 * @since 2026-03-02
 */
public interface PersistentStateComponentAsync<T> extends PersistentStateComponent<T> {
    Coroutine<?, T> getStateAsync();

    @Override
    default @Nullable T getState() {
        throw new IllegalStateException("Use getStateAsync() instead");
    }
}
