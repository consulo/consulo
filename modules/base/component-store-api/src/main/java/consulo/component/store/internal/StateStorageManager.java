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

import consulo.component.persist.RoamingType;
import consulo.component.persist.Storage;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface StateStorageManager {
  void addMacro(String macro, String expansion);

  
  String buildFileSpec(Storage storage);

  @Nullable TrackingPathMacroSubstitutor getMacroSubstitutor();

  @Nullable StateStorage getStateStorage(Storage storageSpec);

  @Nullable StateStorage getStateStorage(String fileSpec, RoamingType roamingType);

  
  Collection<String> getStorageFileNames();

  void clearStateStorage(String file);

  @Nullable ExternalizationSession startExternalization();

  
  String expandMacros(String file);

  
  String collapseMacros(String path);

  void setStreamProvider(@Nullable StreamProvider streamProvider);

  @Nullable StreamProvider getStreamProvider();

  interface ExternalizationSession {
    void setState(Storage[] storageSpecs, Object component, String componentName, Object state);

    /**
     * return empty list if nothing to save
     * @param force
     */
    
    List<StateStorage.SaveSession> createSaveSessions(boolean force);
  }
}