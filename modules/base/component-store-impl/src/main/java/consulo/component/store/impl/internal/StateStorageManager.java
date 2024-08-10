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

import consulo.component.persist.RoamingType;
import consulo.component.persist.Storage;
import consulo.component.store.impl.internal.storage.StateStorage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public interface StateStorageManager {
  void addMacro(@Nonnull String macro, @Nonnull String expansion);

  @Nonnull
  String buildFileSpec(@Nonnull Storage storage);

  @Nullable
  TrackingPathMacroSubstitutor getMacroSubstitutor();

  @Nullable
  StateStorage getStateStorage(@Nonnull Storage storageSpec);

  @Nullable
  StateStorage getStateStorage(@Nonnull String fileSpec, @Nonnull RoamingType roamingType);

  @Nonnull
  Collection<String> getStorageFileNames();

  void clearStateStorage(@Nonnull String file);

  @Nullable
  ExternalizationSession startExternalization();

  @Nonnull
  String expandMacros(@Nonnull String file);

  @Nonnull
  String collapseMacros(@Nonnull String path);

  void setStreamProvider(@Nullable StreamProvider streamProvider);

  @Nullable
  StreamProvider getStreamProvider();

  interface ExternalizationSession {
    void setState(@Nonnull Storage[] storageSpecs, @Nonnull Object component, @Nonnull String componentName, @Nonnull Object state);

    /**
     * return empty list if nothing to save
     * @param force
     */
    @Nonnull
    List<StateStorage.SaveSession> createSaveSessions(boolean force);
  }
}