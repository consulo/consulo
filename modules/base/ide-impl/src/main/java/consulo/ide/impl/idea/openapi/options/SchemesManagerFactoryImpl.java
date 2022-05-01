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
package consulo.ide.impl.idea.openapi.options;

import consulo.component.persist.RoamingType;
import consulo.ide.impl.idea.openapi.components.ServiceBean;
import consulo.component.persist.SettingsSavingComponent;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.impl.internal.store.IApplicationStore;
import consulo.component.store.impl.internal.StreamProvider;
import consulo.component.store.impl.internal.StateStorageManager;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.util.List;

@Singleton
public class SchemesManagerFactoryImpl extends SchemesManagerFactory implements SettingsSavingComponent {
  private static final Logger LOG = Logger.getInstance(SchemesManagerFactoryImpl.class);

  private final List<SchemesManagerImpl> myRegisteredManagers = ContainerUtil.createLockFreeCopyOnWriteList();

  private final IApplicationStore myApplicationStore;

  @Inject
  public SchemesManagerFactoryImpl(IApplicationStore applicationStore) {
    myApplicationStore = applicationStore;
  }

  @Override
  public <T, E extends ExternalizableScheme> SchemesManager<T, E> createSchemesManager(final String fileSpec, final SchemeProcessor<T, E> processor, final RoamingType roamingType) {
    StateStorageManager stateStorageManager = myApplicationStore.getStateStorageManager();
    
    String baseDirPath = stateStorageManager.expandMacros(fileSpec);
    StreamProvider provider = stateStorageManager.getStreamProvider();
    SchemesManagerImpl<T, E> manager = new SchemesManagerImpl<>(fileSpec, processor, roamingType, provider, new File(baseDirPath));
    myRegisteredManagers.add(manager);
    return manager;
  }

  @Override
  public void updateConfigFilesFromStreamProviders() {
    ServiceBean.loadServicesFromBeans(SCHEME_OWNER, Object.class);
    for (SchemesManagerImpl registeredManager : myRegisteredManagers) {
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
    ServiceBean.loadServicesFromBeans(SCHEME_OWNER, Object.class);
    for (SchemesManager registeredManager : myRegisteredManagers) {
      try {
        registeredManager.save();
      }
      catch (Throwable e) {
        LOG.info("Cannot save settings for " + registeredManager.getClass().getName(), e);
      }
    }
  }
}
