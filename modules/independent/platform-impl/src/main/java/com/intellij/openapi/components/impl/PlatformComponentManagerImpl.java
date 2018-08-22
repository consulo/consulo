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
package com.intellij.openapi.components.impl;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.components.ServiceDescriptor;
import com.intellij.openapi.components.impl.stores.IComponentStore;
import com.intellij.util.io.storage.HeavyProcessLatch;
import consulo.components.impl.stores.StateComponentInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class PlatformComponentManagerImpl extends ComponentManagerImpl {
  private boolean myHandlingInitComponentError;

  protected PlatformComponentManagerImpl(ComponentManager parent) {
    super(parent);
  }

  protected PlatformComponentManagerImpl(ComponentManager parent, @Nonnull String name) {
    super(parent, name);
  }

  @Override
  public boolean initializeIfStorableComponent(@Nonnull Object component, boolean service) {
    IComponentStore stateStore = getStateStore();
    if(stateStore != null) {
      StateComponentInfo<Object> info = stateStore.loadStateIfStorable(component);
      if(info != null) {
        info.getComponent().callAfterLoadState();
        return true;
      }
    }

    return false;
  }

  @Nullable
  protected IComponentStore getStateStore() {
    return null;
  }

  @Override
  protected <T> T runServiceInitialize(@Nonnull ServiceDescriptor descriptor, @Nonnull Supplier<T> runnable) {
    // prevent storages from flushing and blocking FS
    try(AccessToken ignored = HeavyProcessLatch.INSTANCE.processStarted("Creating component '" + descriptor.getImplementation() + "'")) {
      return super.runServiceInitialize(descriptor, runnable);
    }
  }

  @Override
  protected void handleInitComponentError(@Nonnull Throwable ex, @Nullable String componentClassName, @Nullable ComponentConfig config) {
    if (!myHandlingInitComponentError) {
      myHandlingInitComponentError = true;
      try {
        PluginManager.handleComponentError(ex, componentClassName, config);
      }
      finally {
        myHandlingInitComponentError = false;
      }
    }
  }
}
