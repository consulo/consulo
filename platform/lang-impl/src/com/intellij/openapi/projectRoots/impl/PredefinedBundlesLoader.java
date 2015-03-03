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
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.bundle.PredefinedBundlesProvider;

/**
 * @author VISTALL
 * @since 15:05/22.11.13
 */
public class PredefinedBundlesLoader implements ApplicationComponent {
  @Override
  public void initComponent() {
    Consumer<SdkImpl> consumer = new Consumer<SdkImpl>() {
      @Override
      public void consume(final SdkImpl sdk) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            sdk.setBundled(true);
            SdkTable.getInstance().addSdk(sdk);
          }
        });
      }
    };

    for (PredefinedBundlesProvider predefinedBundlesProvider : PredefinedBundlesProvider.EP_NAME.getExtensions()) {
      predefinedBundlesProvider.createBundles(consumer);
    }
  }

  @Override
  public void disposeComponent() {
  }

  @NotNull
  @Override
  public String getComponentName() {
    return getClass().getCanonicalName();
  }
}
