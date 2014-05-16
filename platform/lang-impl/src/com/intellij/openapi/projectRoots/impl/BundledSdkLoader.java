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

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.projectRoots.BundledSdkProvider;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 15:05/22.11.13
 */
public class BundledSdkLoader implements ApplicationComponent {

  @Override
  public void initComponent() {
    new WriteAction<Object>()
    {
      @Override
      protected void run(Result<Object> result) throws Throwable {
        for (BundledSdkProvider bundledSdkProvider : BundledSdkProvider.EP_NAME.getExtensions()) {
          final Sdk[] bundledSdks = bundledSdkProvider.createBundledSdks();

          for (Sdk bundledSdk : bundledSdks) {
            if(bundledSdk instanceof SdkImpl) {
              ((SdkImpl)bundledSdk).setBundled(true);
            }

            SdkTable.getInstance().addSdk(bundledSdk);
          }
        }
      }
    }.execute();
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
