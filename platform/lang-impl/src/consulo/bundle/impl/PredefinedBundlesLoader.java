/*
 * Copyright 2013 must-be.org
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
package consulo.bundle.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.util.Consumer;
import com.intellij.util.SystemProperties;
import consulo.annotations.RequiredDispatchThread;
import consulo.bundle.PredefinedBundlesProvider;

/**
 * @author VISTALL
 * @since 15:05/22.11.13
 */
public class PredefinedBundlesLoader extends ApplicationComponent.Adapter {
  @Override
  public void initComponent() {
    if (SystemProperties.is("disable.predefined.bundles")) {
      return;
    }

    Consumer<SdkImpl> consumer = new Consumer<SdkImpl>() {
      @Override
      @RequiredDispatchThread
      public void consume(final SdkImpl sdk) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            sdk.setPredefined(true);
            SdkTable.getInstance().addSdk(sdk);
          }
        });
      }
    };

    for (PredefinedBundlesProvider predefinedBundlesProvider : PredefinedBundlesProvider.EP_NAME.getExtensions()) {
      predefinedBundlesProvider.createBundles(consumer);
    }
  }
}
