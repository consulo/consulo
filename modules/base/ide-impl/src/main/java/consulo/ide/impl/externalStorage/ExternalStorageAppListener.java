/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.externalStorage;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.event.ApplicationLoadListener;
import consulo.application.Application;
import consulo.application.impl.internal.store.IApplicationStore;
import consulo.component.store.impl.internal.StateStorageManager;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.ide.impl.externalStorage.storage.ExternalStorage;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11-Feb-17
 */
@ExtensionImpl
public class ExternalStorageAppListener implements ApplicationLoadListener {
  private final Application myApplication;
  @Nonnull
  private final ExternalServiceConfiguration myExternalServiceConfiguration;
  private final IApplicationStore myApplicationStore;

  @Inject
  public ExternalStorageAppListener(@Nonnull Application application, @Nonnull ExternalServiceConfiguration externalServiceConfiguration, IApplicationStore applicationStore) {
    myExternalServiceConfiguration = externalServiceConfiguration;
    myApplicationStore = applicationStore;
    myApplication = application;
  }

  @Override
  public void beforeApplicationLoaded() {
    StateStorageManager stateStorageManager = myApplicationStore.getStateStorageManager();

    ExternalStorage storage = new ExternalStorage();

    ExternalStoragePluginManager pluginManager = new ExternalStoragePluginManager(myApplication, myExternalServiceConfiguration);

    ExternalStorageManager storageManager = new ExternalStorageManager(myApplication, myApplicationStore, storage, pluginManager);

    ExternalStorageStreamProvider provider = new ExternalStorageStreamProvider(storage, myExternalServiceConfiguration);

    stateStorageManager.setStreamProvider(provider);

    if (provider.isEnabled()) {
      storageManager.startChecking();
    }
  }
}
