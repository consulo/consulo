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

import consulo.component.persist.Storage;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public interface StateStorage {
  @Nullable
  <T> T getState(@Nullable Object component, String componentName, Class<T> stateClass) throws StateStorageException;

  boolean hasState(@Nullable Object component, String componentName, Class<?> aClass, boolean reloadData);

  @Nullable
  ExternalizationSession startExternalization();

  /**
   * Get changed component names
   */
  void analyzeExternalChangesAndUpdateIfNeed(Set<String> result);

  interface ExternalizationSession {
    void setState(Object component, String componentName, Object state, @Nullable Storage storageSpec);

    /**
     * return null if nothing to save
     * @param force - ignore store check
     */
    @Nullable
    SaveSession createSaveSession(boolean force);
  }

  interface SaveSession {
    void save(boolean force);
  }
}
