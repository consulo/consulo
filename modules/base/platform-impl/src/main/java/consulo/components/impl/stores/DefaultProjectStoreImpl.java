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

import com.intellij.openapi.components.*;
import com.intellij.openapi.components.StateStorage.SaveSession;
import com.intellij.openapi.components.impl.ProjectPathMacroManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.DefaultProjectImpl;
import com.intellij.openapi.util.Couple;
import com.intellij.util.containers.ContainerUtil;
import consulo.components.impl.stores.storage.StateStorageManager;
import consulo.components.impl.stores.storage.StorageData;
import consulo.components.impl.stores.storage.VfsFileBasedStorage;
import consulo.components.impl.stores.storage.XmlElementStorage;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Singleton
public class DefaultProjectStoreImpl extends ProjectStoreImpl {
  @NonNls
  private static final String ROOT_TAG_NAME = "defaultProject";

  @Inject
  public DefaultProjectStoreImpl(@Nonnull Project project, @Nonnull ProjectPathMacroManager pathMacroManager, @Nonnull Provider<ApplicationDefaultStoreCache> applicationDefaultStoreCache) {
    super(project, pathMacroManager, applicationDefaultStoreCache);
  }

  @Nullable
  Element getStateCopy() {
    final Element element = getProject().getStateElement();
    return element != null ? element.clone() : null;
  }

  @Override
  @Nonnull
  protected DefaultProjectImpl getProject() {
    return (DefaultProjectImpl)super.getProject();
  }

  @Nonnull
  @Override
  protected StateStorageManager createStateStorageManager() {
    final XmlElementStorage storage = new XmlElementStorage("", RoamingType.DISABLED, myPathMacroManager.createTrackingSubstitutor(), ROOT_TAG_NAME, null) {
      @Override
      @Nullable
      protected Element loadLocalData() {
        return getStateCopy();
      }

      @Override
      protected XmlElementStorageSaveSession createSaveSession(@Nonnull StorageData storageData) {
        return new XmlElementStorageSaveSession(storageData) {
          @Override
          protected void doSave(@Nullable Element element) {
            // we must set empty element instead of null as indicator - ProjectManager state is ready to save
            getProject().setStateElement(element == null ? new Element("empty") : element);
          }

          // we must not collapse paths here, because our solution is just a big hack
          // by default, getElementToSave() returns collapsed paths -> setDefaultProjectRootElement -> project manager writeExternal -> save -> compare old and new - diff because old has expanded, but new collapsed
          // -> needless save
          @Override
          protected boolean isCollapsePathsOnSave() {
            return false;
          }
        };
      }
    };

    //noinspection deprecation
    return new StateStorageManager() {
      @Override
      public void addMacro(@Nonnull String macro, @Nonnull String expansion) {
        throw new UnsupportedOperationException("Method addMacro not implemented in " + getClass());
      }

      @Nonnull
      @Override
      public String buildFileSpec(@Nonnull Storage storage) {
        throw new UnsupportedOperationException("Method buildFileSpec not implemented in " + getClass());
      }

      @Override
      @Nullable
      public TrackingPathMacroSubstitutor getMacroSubstitutor() {
        return null;
      }

      @Override
      @Nullable
      public StateStorage getStateStorage(@Nonnull Storage storageSpec) throws StateStorageException {
        return storage;
      }

      @Nullable
      @Override
      public StateStorage getStateStorage(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
        return storage;
      }

      @Nonnull
      @Override
      public Couple<Collection<VfsFileBasedStorage>> getCachedFileStateStorages(@Nonnull Collection<String> changed, @Nonnull Collection<String> deleted) {
        return new Couple<>(Collections.<VfsFileBasedStorage>emptyList(), Collections.<VfsFileBasedStorage>emptyList());
      }

      @Override
      public void clearStateStorage(@Nonnull String file) {
      }

      @Nullable
      @Override
      public ExternalizationSession startExternalization() {
        StateStorage.ExternalizationSession externalizationSession = storage.startExternalization();
        return externalizationSession == null ? null : new MyExternalizationSession(externalizationSession);
      }

      @Nonnull
      @Override
      public String expandMacros(@Nonnull String file) {
        throw new UnsupportedOperationException("Method expandMacros not implemented in " + getClass());
      }

      @Nonnull
      @Override
      public String collapseMacros(@Nonnull String path) {
        throw new UnsupportedOperationException("Method collapseMacros not implemented in " + getClass());
      }

      @Override
      public void setStreamProvider(@Nullable StreamProvider streamProvider) {
        throw new UnsupportedOperationException("Method setStreamProvider not implemented in " + getClass());
      }

      @Nullable
      @Override
      public StreamProvider getStreamProvider() {
        throw new UnsupportedOperationException("Method getStreamProviders not implemented in " + getClass());
      }

      @Nonnull
      @Override
      public Collection<String> getStorageFileNames() {
        throw new UnsupportedOperationException("Method getStorageFileNames not implemented in " + getClass());
      }
    };
  }

  @Override
  public void load() throws IOException {
    Element stateElement = getProject().getStateElement();
    if (stateElement != null) {
      // reload storage - since initNotLazyService called before #loadState()
      reload(Arrays.asList(getMainStorage()));
    }
  }

  private static class MyExternalizationSession implements StateStorageManager.ExternalizationSession {
    @Nonnull
    final StateStorage.ExternalizationSession externalizationSession;

    public MyExternalizationSession(@Nonnull StateStorage.ExternalizationSession externalizationSession) {
      this.externalizationSession = externalizationSession;
    }

    @Override
    public void setState(@Nonnull Storage[] storageSpecs, @Nonnull Object component, @Nonnull String componentName, @Nonnull Object state) {
      externalizationSession.setState(component, componentName, state, null);
    }

    @Nonnull
    @Override
    public List<SaveSession> createSaveSessions(boolean force) {
      return ContainerUtil.createMaybeSingletonList(externalizationSession.createSaveSession(false));
    }
  }
}