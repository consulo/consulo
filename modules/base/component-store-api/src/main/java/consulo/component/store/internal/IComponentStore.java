/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.component.store.internal;

import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IComponentStore {
  class SaveCancelledException extends RuntimeException {
    public SaveCancelledException() {
    }
  }

  void load() throws IOException, StateStorageException;

  Coroutine<Object, Object> createSaveCoroutine(List<Pair<StateStorage.SaveSession, File>> readonlyFiles);

  Continuation<?> saveAsync(UIAccess uiAccess, List<Pair<StateStorage.SaveSession, File>> readonlyFiles);

  /**
   * Return storable info about component. Throws for a {@code PersistentStateComponentAsync}, which must be
   * loaded through {@link #loadStateIfStorableAsync(Object)}.
   */
  @Nullable StateComponentInfo loadStateIfStorable(Object component);

  /**
   * Asynchronous counterpart of {@link #loadStateIfStorable(Object)}. The returned coroutine produces
   * {@code null} when the component is not storable.
   */
  Coroutine<?, StateComponentInfo> loadStateIfStorableAsync(Object component);

  /**
   * Reloads the named components from their storages.
   *
   * @return a continuation of the running reload, so callers can sequence work after it
   */
  Continuation<?> reinitComponents(Set<String> componentNames);


  StateStorageManager getStateStorageManager();

  /**
   * @return a continuation of the running reload, or null when nothing changed
   */
  @Nullable Continuation<?> reload(Collection<? extends StateStorage> changedStorages);

  /**
   * The context asynchronous state work runs in. Carries the executor and {@code UIAccess.KEY}, so coroutine
   * steps bound to the UI thread can hop to it.
   */
  CoroutineContext createCoroutineContext();
}
