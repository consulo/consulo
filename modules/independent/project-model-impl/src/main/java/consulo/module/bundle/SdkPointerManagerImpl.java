/*
 * Copyright 2013-2016 consulo.io
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
package consulo.module.bundle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import consulo.bundle.SdkTableListener;
import consulo.bundle.SdkPointerManager;
import consulo.util.pointers.NamedPointerManagerImpl;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 17:53/15.06.13
 */
public class SdkPointerManagerImpl extends NamedPointerManagerImpl<Sdk> implements SdkPointerManager {
  public SdkPointerManagerImpl() {
    ApplicationManager.getApplication().getMessageBus().connect().subscribe(SdkTable.SDK_TABLE_TOPIC, new SdkTableListener.Adapter() {
      @Override
      public void sdkAdded(@Nonnull Sdk sdk) {
        updatePointers(sdk);
      }

      @Override
      public void sdkRemoved(@Nonnull Sdk sdk) {
        unregisterPointer(sdk);
      }

      @Override
      public void sdkNameChanged(@Nonnull Sdk sdk, @Nonnull String previousName) {
        updatePointers(sdk, previousName);
      }
    });
  }

  @Nullable
  @Override
  public Sdk findByName(@Nonnull String name) {
    return SdkTable.getInstance().findSdk(name);
  }
}
