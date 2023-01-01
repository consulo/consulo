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
package consulo.component.store.impl.internal;

import consulo.component.macro.PathMacroManager;
import consulo.component.store.impl.internal.storage.XmlElementStorage;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.io.IOException;

public abstract class BaseFileConfigurableStoreImpl extends ComponentStoreImpl {
  public static final String ATTRIBUTE_NAME = "name";

  private StateStorageManager myStateStorageManager;
  protected final Provider<? extends PathMacroManager> myPathMacroManager;

  protected BaseFileConfigurableStoreImpl(@Nonnull Provider<ApplicationDefaultStoreCache> applicationDefaultStoreCache, @Nonnull Provider<? extends PathMacroManager> pathMacroManager) {
    super(applicationDefaultStoreCache);
    myPathMacroManager = pathMacroManager;
  }

  @Nonnull
  protected abstract XmlElementStorage getMainStorage();

  @Override
  public void load() throws IOException, StateStorageException {
    getMainStorage().getStorageData();
  }

  @Nonnull
  @Override
  protected final PathMacroManager getPathMacroManagerForDefaults() {
    return myPathMacroManager.get();
  }

  @Nonnull
  @Override
  public final StateStorageManager getStateStorageManager() {
    if (myStateStorageManager == null) {
      myStateStorageManager = createStateStorageManager();
    }
    return myStateStorageManager;
  }

  @Nonnull
  protected abstract StateStorageManager createStateStorageManager();
}
