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
package consulo.components.impl.stores;

import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.StateStorageException;
import com.intellij.openapi.util.Pair;
import consulo.annotation.access.RequiredWriteAction;
import consulo.components.impl.stores.storage.StateStorageManager;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  void save(boolean force, @Nonnull List<Pair<StateStorage.SaveSession, File>> readonlyFiles);

  @RequiredWriteAction
  void saveAsync(@Nonnull UIAccess uiAccess, @Nonnull List<Pair<StateStorage.SaveSession, File>> readonlyFiles);

  /**
   * Return storable info about component
   */
  @Nullable
  <T> StateComponentInfo<T> loadStateIfStorable(@Nonnull T component);

  void reinitComponents(@Nonnull Set<String> componentNames, boolean reloadData);

  @Nonnull
  StateStorageManager getStateStorageManager();

  /**
   * @return true is reloaded - false if not reloaded
   */
  boolean reload(@Nonnull Collection<? extends StateStorage> changedStorages);
}
