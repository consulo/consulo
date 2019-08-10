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
package consulo.components.impl.stores.storage;

import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.StateStorageException;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import consulo.logging.Logger;
import consulo.components.impl.stores.DefaultStateSerializer;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class StateStorageBase<T extends StorageDataBase> implements StateStorage {
  protected static final Logger LOG = Logger.getInstance(StateStorageBase.class);

  private boolean mySavingDisabled = false;
  protected final TrackingPathMacroSubstitutor myPathMacroSubstitutor;

  protected StateStorageBase(@Nullable TrackingPathMacroSubstitutor trackingPathMacroSubstitutor) {
    myPathMacroSubstitutor = trackingPathMacroSubstitutor;
  }

  @Override
  @Nullable
  public final <S> S getState(Object component, @Nonnull String componentName, @Nonnull Class<S> stateClass) throws StateStorageException {
    return DefaultStateSerializer.deserializeState(getStateAndArchive(getStorageData(), componentName), stateClass);
  }

  @Nullable
  protected abstract Element getStateAndArchive(@Nonnull T storageData, @Nonnull String componentName);

  @Override
  public final boolean hasState(@Nullable Object component, @Nonnull String componentName, Class<?> aClass, boolean reloadData) {
    return getStorageData(reloadData).hasState(componentName);
  }

  @Nonnull
  public final T getStorageData() {
    return getStorageData(false);
  }

  protected abstract T getStorageData(boolean reloadData);

  public final void disableSaving() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Disabled saving for " + toString());
    }
    mySavingDisabled = true;
  }

  public final void enableSaving() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Enabled saving " + toString());
    }
    mySavingDisabled = false;
  }

  protected final boolean checkIsSavingDisabled() {
    if (mySavingDisabled && LOG.isDebugEnabled()) {
      LOG.debug("Saving disabled for " + toString());
    }
    return mySavingDisabled;
  }
}