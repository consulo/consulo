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
package consulo.content.impl.internal.bundle;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.impl.util.NamedPointerManagerImpl;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkPointerManager;
import consulo.content.bundle.SdkTable;
import consulo.content.bundle.event.SdkTableListener;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 17:53/15.06.13
 */
@Singleton
@ServiceImpl
public class SdkPointerManagerImpl extends NamedPointerManagerImpl<Sdk> implements SdkPointerManager {
  @Nonnull
  private final Provider<SdkTable> mySdkTableProvider;

  @Inject
  public SdkPointerManagerImpl(@Nonnull Application application, @Nonnull Provider<SdkTable> sdkTableProvider) {
    mySdkTableProvider = sdkTableProvider;
    application.getMessageBus().connect().subscribe(SdkTableListener.class, new SdkTableListener() {
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
  protected Sdk findByName(@Nonnull String name) {
    return mySdkTableProvider.get().findSdk(name);
  }

  public void updatePointers(@Nonnull List<? extends Sdk> sdks) {
    for (Sdk sdk : sdks) {
      updatePointers(sdk);
    }
  }
}
