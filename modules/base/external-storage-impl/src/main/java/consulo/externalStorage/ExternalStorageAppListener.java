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
package consulo.externalStorage;

import com.intellij.ide.ApplicationLoadListener;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.impl.stores.IApplicationStore;
import com.intellij.openapi.components.impl.stores.StateStorageManager;
import consulo.application.ex.ApplicationEx2;
import consulo.externalStorage.storage.ExternalStorage;

import javax.inject.Inject;

/**
 * @author VISTALL
 * @since 11-Feb-17
 */
public class ExternalStorageAppListener implements ApplicationLoadListener {
  private final Application myApplication;

  @Inject
  public ExternalStorageAppListener(Application application) {
    myApplication = application;
  }

  @Override
  public void beforeApplicationLoaded(Application application) {
    ApplicationEx2 applicationEx = (ApplicationEx2)myApplication;

    IApplicationStore stateStore = applicationEx.getStateStore();

    StateStorageManager stateStorageManager = stateStore.getStateStorageManager();

    ExternalStorage storage = new ExternalStorage();

    stateStorageManager.setStreamProvider(new ExternalStorageStreamProvider(storage, stateStorageManager));
  }
}
