/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.application.impl.internal.store;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.RoamingType;
import consulo.component.persist.SettingsSavingComponent;
import consulo.component.persist.scheme.ExternalizableScheme;
import consulo.component.persist.scheme.SchemeManager;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.component.persist.scheme.SchemeProcessor;
import consulo.component.store.internal.StateStorageManager;
import consulo.component.store.internal.StreamProvider;
import consulo.component.store.impl.internal.scheme.SchemeManagerImpl;
import consulo.logging.Logger;
import consulo.util.collection.Lists;
import consulo.virtualFileSystem.internal.VirtualFileTracker;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.util.List;

@Singleton
@ServiceImpl
public class SchemeManagerFactoryImpl extends SchemeManagerFactory implements SettingsSavingComponent {
  private static final Logger LOG = Logger.getInstance(SchemeManagerFactoryImpl.class);

  private final List<SchemeManagerImpl> myRegisteredManagers = Lists.newLockFreeCopyOnWriteList();

  private final IApplicationStore myApplicationStore;
  private final VirtualFileTracker myVirtualFileTracker;

  @Inject
  public SchemeManagerFactoryImpl(IApplicationStore applicationStore, VirtualFileTracker virtualFileTracker) {
    myApplicationStore = applicationStore;
    myVirtualFileTracker = virtualFileTracker;
  }

  @Override
  public <T, E extends ExternalizableScheme> SchemeManager<T, E> createSchemeManager(final String fileSpec, final SchemeProcessor<T, E> processor, final RoamingType roamingType) {
    StateStorageManager stateStorageManager = myApplicationStore.getStateStorageManager();
    
    String baseDirPath = stateStorageManager.expandMacros(fileSpec);
    StreamProvider provider = stateStorageManager.getStreamProvider();
    SchemeManagerImpl<T, E> manager = new SchemeManagerImpl<>(fileSpec, myVirtualFileTracker, processor, roamingType, provider, new File(baseDirPath));
    myRegisteredManagers.add(manager);
    return manager;
  }

  @Override
  public void updateConfigFilesFromStreamProviders() {
    for (SchemeManagerImpl registeredManager : myRegisteredManagers) {
      try {
        registeredManager.updateConfigFilesFromStreamProviders();
      }
      catch (Throwable e) {
        LOG.info("Cannot save settings for " + registeredManager.getClass().getName(), e);
      }
    }
  }

  @Override
  public void save() {
    for (SchemeManager registeredManager : myRegisteredManagers) {
      try {
        registeredManager.save();
      }
      catch (Throwable e) {
        LOG.info("Cannot save settings for " + registeredManager.getClass().getName(), e);
      }
    }
  }
}
