/*
 * Copyright 2013 Consulo.org
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
package org.consulo.sdk.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import org.consulo.sdk.SdkPointerManager;
import org.consulo.util.pointers.NamedPointerManagerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 17:53/15.06.13
 */
public class SdkPointerManagerImpl extends NamedPointerManagerImpl<Sdk> implements SdkPointerManager {
  public SdkPointerManagerImpl() {
    ApplicationManager.getApplication().getMessageBus().connect().subscribe(SdkTable.SDK_TABLE_TOPIC, new SdkTable.Listener() {
      @Override
      public void sdkAdded(Sdk sdk) {
        updatePointers(sdk);
      }

      @Override
      public void sdkRemoved(Sdk sdk) {
        unregisterPointer(sdk);
      }

      @Override
      public void sdkNameChanged(Sdk sdk, String previousName) {
        updatePointers(sdk);
      }
    });
  }

  @Nullable
  @Override
  public Sdk findByName(@NotNull String name) {
    return SdkTable.getInstance().findSdk(name);
  }
}
