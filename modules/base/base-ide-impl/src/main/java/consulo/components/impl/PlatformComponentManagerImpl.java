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
package consulo.components.impl;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.components.ServiceDescriptor;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.extensions.impl.ExtensionAreaId;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.util.io.storage.HeavyProcessLatch;
import consulo.components.impl.stores.IComponentStore;
import consulo.components.impl.stores.StateComponentInfo;
import consulo.container.plugin.ComponentConfig;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public abstract class PlatformComponentManagerImpl extends ComponentManagerImpl {
  private static final Logger LOG = Logger.getInstance(PlatformComponentManagerImpl.class);

  private boolean myHandlingInitComponentError;
  private AtomicInteger myCreatedNotLazyServicesCount = new AtomicInteger();

  protected PlatformComponentManagerImpl(@Nullable ComponentManager parent, @Nonnull String name, @Nullable ExtensionAreaId areaId) {
    super(parent, name, areaId, true);
  }

  protected void notifyAboutInitialization(float percentOfLoad, Object component) {
  }

  @Override
  public boolean initializeIfStorableComponent(@Nonnull Object component, boolean service, boolean lazy) {
    if (!lazy) {
      myCreatedNotLazyServicesCount.incrementAndGet();
    }

    boolean result = false;
    IComponentStore stateStore = getStateStore();
    if (stateStore != null) {
      StateComponentInfo<Object> info = stateStore.loadStateIfStorable(component);
      if (info != null) {
        if (Application.get().isWriteAccessAllowed()) {
          LOG.warn(new IllegalArgumentException("Getting service from write-action leads to possible deadlock. Service implementation " + component.getClass().getName()));
        }

        info.getComponent().afterLoadState();
        result = true;
      }
    }

    if (!lazy) {
      notifyAboutInitialization(getPercentageOfComponentsLoaded(), component);
    }

    return result;
  }

  private float getPercentageOfComponentsLoaded() {
    return ((float)myCreatedNotLazyServicesCount.get()) / getNotLazyServicesCount();
  }

  @RequiredUIAccess
  @Override
  public synchronized void dispose() {
    myCreatedNotLazyServicesCount.set(0);
    super.dispose();
  }

  @Override
  protected void checkCanceled() {
    ProgressIndicatorProvider provider = getProgressIndicatorProvider();
    if(provider != null) {
      provider.checkForCanceled();
    }
  }

  @Nullable
  protected ProgressIndicatorProvider getProgressIndicatorProvider() {
    if(myParent instanceof PlatformComponentManagerImpl) {
      return ((PlatformComponentManagerImpl)myParent).getProgressIndicatorProvider();
    }
    return null;
  }

  @Nullable
  protected IComponentStore getStateStore() {
    return null;
  }

  @Override
  protected <T> T runServiceInitialize(@Nonnull ServiceDescriptor descriptor, @Nonnull Supplier<T> runnable) {
    // prevent storages from flushing and blocking FS
    try (AccessToken ignored = HeavyProcessLatch.INSTANCE.processStarted("Creating component '" + descriptor.getImplementation() + "'")) {
      return super.runServiceInitialize(descriptor, runnable);
    }
  }

  @Override
  protected void handleInitComponentError(@Nonnull Throwable ex, @Nullable Class componentClass, @Nullable ComponentConfig config) {
    if (!myHandlingInitComponentError) {
      myHandlingInitComponentError = true;
      try {
        PluginManager.handleComponentError(ex, componentClass, config);
      }
      finally {
        myHandlingInitComponentError = false;
      }
    }
  }
}
