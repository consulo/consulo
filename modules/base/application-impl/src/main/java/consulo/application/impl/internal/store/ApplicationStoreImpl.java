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
package consulo.application.impl.internal.store;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.impl.internal.macro.ApplicationPathMacroManagerImpl;
import consulo.application.impl.internal.macro.PathMacrosImpl;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.StateSplitterEx;
import consulo.component.persist.StoragePathMacros;
import consulo.component.store.impl.internal.*;
import consulo.component.store.impl.internal.storage.DirectoryStorageData;
import consulo.component.store.impl.internal.storage.StateStorageFacade;
import consulo.component.store.impl.internal.storage.StateStorageManagerImpl;
import consulo.component.store.internal.*;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.io.IOException;

@Singleton
@ServiceImpl
public class ApplicationStoreImpl extends ComponentStoreImpl implements IApplicationStore {
  private static final Logger LOG = Logger.getInstance(ApplicationStoreImpl.class);

  private static final String ROOT_ELEMENT_NAME = "application";

  private final Application myApplication;
  private final StateStorageManager myStateStorageManager;

  private String myConfigPath;

  @Inject
  public ApplicationStoreImpl(Application application, Provider<ApplicationDefaultStoreCache> applicationDefaultStoreCache) {
    super(applicationDefaultStoreCache);
    myApplication = application;

    SystemOnlyPathMacros macros = new SystemOnlyPathMacros();
    SystemOnlyPathMacrosService pathMacrosService = new SystemOnlyPathMacrosService(macros);
    ApplicationPathMacroManagerImpl pathMacroManager = new ApplicationPathMacroManagerImpl(macros);

    myStateStorageManager = new StateStorageManagerImpl(
        new TrackingPathMacroSubstitutorImpl(() -> pathMacroManager),
        ROOT_ELEMENT_NAME,
        application,
        () -> null,
        () -> pathMacrosService,
        StateStorageFacade.JAVA_IO
      ) {
      
      @Override
      protected String getConfigurationMacro(boolean directorySpec) {
        return directorySpec ? StoragePathMacros.ROOT_CONFIG : StoragePathMacros.APP_CONFIG;
      }

      
      @Override
      public StateSplitterEx createSplitter(Class<? extends StateSplitterEx> splitter) {
        return application.getUnbindedInstance(splitter);
      }

      @Override
      protected TrackingPathMacroSubstitutor getMacroSubstitutor(String fileSpec) {
        if (fileSpec.equals(StoragePathMacros.APP_CONFIG + '/' + PathMacrosImpl.STORE_FILE)) return null;
        return super.getMacroSubstitutor(fileSpec);
      }

      @Override
      protected boolean isUseXmlProlog() {
        return false;
      }
    };
  }

  @Override
  public void load() throws IOException {
    long t = System.currentTimeMillis();
    myApplication.initNotLazyServices();
    t = System.currentTimeMillis() - t;
    LOG.info(myApplication.getNotLazyServicesCount() + " application services initialized in " + t + " ms");
  }

  @Override
  public void setOptionsPath(String path) {
    myStateStorageManager.addMacro(StoragePathMacros.APP_CONFIG, path);
    myStateStorageManager.addMacro(StoragePathMacros.DEFAULT_FILE, path + "/other" + DirectoryStorageData.DEFAULT_EXT);
  }

  @Override
  public void setConfigPath(String configPath) {
    myStateStorageManager.addMacro(StoragePathMacros.ROOT_CONFIG, configPath);
    myConfigPath = configPath;
  }

  @Override
  
  public String getConfigPath() {
    String configPath = myConfigPath;
    if (configPath == null) {
      // unrealistic case, but we keep backward compatibility
      configPath = ContainerPathManager.get().getConfigPath();
    }
    return configPath;
  }

  @Override
  
  protected MessageBus getMessageBus() {
    return myApplication.getMessageBus();
  }

  
  @Override
  public StateStorageManager getStateStorageManager() {
    return myStateStorageManager;
  }
}
