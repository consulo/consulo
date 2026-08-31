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
package consulo.component;

import jakarta.inject.Provider;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link Provider} that can also resolve its instance without blocking the calling thread.
 * <p>
 * Creating a service whose state is loaded asynchronously (see
 * {@code consulo.component.persist.PersistentStateComponentAsync}) requires running a coroutine, so
 * {@link #get()} is illegal on the UI thread. Use {@link #getAsync()} there instead - it returns an already
 * completed future when the instance exists, and otherwise creates it off the UI thread.
 *
 * @author VISTALL
 * @since 2026-07-29
 */
public interface ProviderAsync<T> extends Provider<T> {
    CompletableFuture<T> getAsync();
}
