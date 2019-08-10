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
package com.intellij.openapi.options;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.ServiceBean;
import com.intellij.openapi.components.SettingsSavingComponent;
import consulo.components.impl.stores.StreamProvider;
import consulo.logging.Logger;
import com.intellij.util.containers.ContainerUtil;
import consulo.application.ex.ApplicationEx2;
import consulo.util.pointers.Named;

import javax.inject.Singleton;
import java.io.File;
import java.util.List;

@Singleton
public class SchemesManagerFactoryImpl extends SchemesManagerFactory implements SettingsSavingComponent {
  public static final Logger LOGGER = Logger.getInstance(SchemesManagerFactoryImpl.class);

  private final List<SchemesManagerImpl> myRegisteredManagers = ContainerUtil.createLockFreeCopyOnWriteList();

  @Override
  public <T extends Named, E extends ExternalizableScheme> SchemesManager<T, E> createSchemesManager(final String fileSpec,
                                                                                                      final SchemeProcessor<E> processor,
                                                                                                      final RoamingType roamingType) {
    final Application application = ApplicationManager.getApplication();
    if (!(application instanceof ApplicationEx2)) return null;
    String baseDirPath = ((ApplicationEx2)application).getStateStore().getStateStorageManager().expandMacros(fileSpec);

    StreamProvider
            provider = ((ApplicationEx2)ApplicationManager.getApplication()).getStateStore().getStateStorageManager().getStreamProvider();
    SchemesManagerImpl<T, E> manager = new SchemesManagerImpl<T, E>(fileSpec, processor, roamingType, provider, new File(baseDirPath));
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
        LOGGER.info("Cannot save settings for " + registeredManager.getClass().getName(), e);
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
        LOGGER.info("Cannot save settings for " + registeredManager.getClass().getName(), e);
      }
    }
  }
}
