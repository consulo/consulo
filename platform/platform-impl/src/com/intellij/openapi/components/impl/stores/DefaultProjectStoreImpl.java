/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.components.impl.stores;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

//todo: extends from base store class
public class DefaultProjectStoreImpl extends ProjectStoreImpl {
  @Nullable private final Element myElement;
  private final ProjectManagerImpl myProjectManager;
  @NonNls private static final String ROOT_TAG_NAME = "defaultProject";

  public DefaultProjectStoreImpl(@NotNull ProjectImpl project, final ProjectManagerImpl projectManager) {
    super(project);

    myProjectManager = projectManager;
    myElement = projectManager.getDefaultProjectRootElement();
  }

  @Nullable
  Element getStateCopy() {
    final Element element = myProjectManager.getDefaultProjectRootElement();
    return element != null ? element.clone() : null;
  }

  @NotNull
  @Override
  protected StateStorageManager createStateStorageManager() {
    Element _d = null;

    if (myElement != null) {
      myElement.detach();
      _d = myElement;
    }

    final ComponentManager componentManager = getComponentManager();
    final PathMacroManager pathMacroManager = PathMacroManager.getInstance(componentManager);

    final Element element = _d;

    final XmlElementStorage storage = new XmlElementStorage("", RoamingType.DISABLED, pathMacroManager.createTrackingSubstitutor(), componentManager,
                                                            ROOT_TAG_NAME, null,
                                                            ComponentVersionProvider.EMPTY) {
      @Override
      @Nullable
      protected Element loadLocalData() {
        return element;
      }

      @Override
      protected MySaveSession createSaveSession(final MyExternalizationSession externalizationSession) {
        return new DefaultSaveSession(externalizationSession);
      }

      @Override
      @NotNull
      protected StorageData createStorageData() {
        return new BaseStorageData(ROOT_TAG_NAME);
      }

      class DefaultSaveSession extends MySaveSession {
        public DefaultSaveSession(MyExternalizationSession externalizationSession) {
          super(externalizationSession);
        }

        @Override
        protected void doSave() throws StateStorageException {
          Element element = getElementToSave();
          myProjectManager.setDefaultProjectRootElement(element == null ? null : element);
        }

        @Override
        public void collectAllStorageFiles(@NotNull List<VirtualFile> files) {
        }
      }
    };

    //noinspection deprecation
    return new StateStorageManager() {
      @Override
      public void addMacro(@NotNull String macro, @NotNull String expansion) {
        throw new UnsupportedOperationException("Method addMacro not implemented in " + getClass());
      }

      @Override
      @Nullable
      public TrackingPathMacroSubstitutor getMacroSubstitutor() {
        return null;
      }

      @Override
      @Nullable
      public StateStorage getStateStorage(@NotNull Storage storageSpec) throws StateStorageException {
        return storage;
      }

      @Nullable
      @Override
      public StateStorage getStateStorage(@NotNull String fileSpec, @NotNull RoamingType roamingType) {
        return storage;
      }

      @NotNull
      @Override
      public Couple<Collection<FileBasedStorage>> getCachedFileStateStorages(@NotNull Collection<String> changed, @NotNull Collection<String> deleted) {
        return new Couple<Collection<FileBasedStorage>>(Collections.<FileBasedStorage>emptyList(), Collections.<FileBasedStorage>emptyList());
      }

      @Override
      @Nullable
      public StateStorage getFileStateStorage(@NotNull String fileSpec) {
        return storage;
      }

      @Override
      public void clearStateStorage(@NotNull String file) {
      }

      @NotNull
      @Override
      public ExternalizationSession startExternalization() {
        return new MyExternalizationSession(storage);
      }

      @NotNull
      @Override
      public SaveSession startSave(@NotNull ExternalizationSession externalizationSession) {
        return new MySaveSession(storage, externalizationSession);
      }

      @Override
      public void finishSave(@NotNull SaveSession saveSession) {
        storage.finishSave(((MySaveSession)saveSession).saveSession);
      }

      @NotNull
      @Override
      public String expandMacros(@NotNull String file) {
        throw new UnsupportedOperationException("Method expandMacros not implemented in " + getClass());
      }

      @NotNull
      @Override
      public String collapseMacros(@NotNull String path) {
        throw new UnsupportedOperationException("Method collapseMacros not implemented in " + getClass());
      }

      @Override
      @Nullable
      public StateStorage getOldStorage(@NotNull Object component, @NotNull String componentName, @NotNull StateStorageOperation operation) {
        return storage;
      }

      @Override
      public void setStreamProvider(@Nullable com.intellij.openapi.components.impl.stores.StreamProvider streamProvider) {
        throw new UnsupportedOperationException("Method setStreamProvider not implemented in " + getClass());
      }

      @Nullable
      @Override
      public com.intellij.openapi.components.impl.stores.StreamProvider getStreamProvider() {
        throw new UnsupportedOperationException("Method getStreamProviders not implemented in " + getClass());
      }

      @NotNull
      @Override
      public Collection<String> getStorageFileNames() {
        throw new UnsupportedOperationException("Method getStorageFileNames not implemented in " + getClass());
      }

      @Override
      public void reset() {
      }
    };
  }

  @Override
  public void load() throws IOException, StateStorageException {
    if (myElement == null) return;
    super.load();
  }

  private static class MyExternalizationSession implements StateStorageManager.ExternalizationSession {
    @NotNull final StateStorage.ExternalizationSession externalizationSession;

    public MyExternalizationSession(@NotNull XmlElementStorage storage) {
      externalizationSession = storage.startExternalization();
    }

    @Override
    public void setState(@NotNull Storage[] storageSpecs, @NotNull Object component, @NotNull String componentName, @NotNull Object state)
            throws StateStorageException {
      externalizationSession.setState(component, componentName, state, null);
    }

    @Override
    public void setStateInOldStorage(@NotNull Object component, @NotNull String componentName, @NotNull Object state) {
      externalizationSession.setState(component, componentName, state, null);
    }
  }

  private static class MySaveSession implements StateStorageManager.SaveSession {
    @NotNull private final StateStorage.SaveSession saveSession;

    public MySaveSession(@NotNull XmlElementStorage storage, @NotNull StateStorageManager.ExternalizationSession externalizationSession) {
      saveSession = storage.startSave(((MyExternalizationSession)externalizationSession).externalizationSession);
    }

    //returns set of component which were changed, null if changes are much more than just component state.
    @Override
    @Nullable
    public Set<String> analyzeExternalChanges(@NotNull Set<Pair<VirtualFile, StateStorage>> files) {
      throw new UnsupportedOperationException("Method analyzeExternalChanges not implemented in " + getClass());
    }

    @Override
    public void collectAllStorageFiles(@NotNull List<VirtualFile> files) {
    }

    @Override
    public void save() throws StateStorageException {
      saveSession.save();
    }
  }
}
