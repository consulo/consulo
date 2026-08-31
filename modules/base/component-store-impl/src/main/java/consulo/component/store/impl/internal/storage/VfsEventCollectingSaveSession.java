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
package consulo.component.store.impl.internal.storage;

import consulo.component.store.internal.StateStorage;
import consulo.virtualFileSystem.event.VFileEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A save session that, instead of firing its VFS content-change events itself, appends them to a shared batch so the
 * store can apply them once, synchronously, after all writes. This keeps the VFS record in sync with lock-free NIO
 * writes before any later filesystem refresh can observe a diff.
 */
public interface VfsEventCollectingSaveSession extends StateStorage.SaveSession {
    /**
     * Persist the state and, when {@code events} is not {@code null}, append the resulting VFS events to it instead of
     * firing them. When {@code events} is {@code null} the session applies its events on its own.
     */
    void save(boolean force, @Nullable List<VFileEvent> events);

    @Override
    default void save(boolean force) {
        save(force, null);
    }
}
