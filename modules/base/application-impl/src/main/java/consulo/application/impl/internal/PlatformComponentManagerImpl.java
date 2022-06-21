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
package consulo.application.impl.internal;

import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.ApplicationProperties;
import consulo.application.impl.internal.performance.HeavyProcessLatch;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.component.ComponentManager;
import consulo.component.bind.InjectingBinding;
import consulo.component.impl.BaseComponentManager;
import consulo.component.impl.extension.ExtensionAreaId;
import consulo.component.store.impl.internal.IComponentStore;
import consulo.component.store.impl.internal.StateComponentInfo;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public abstract class PlatformComponentManagerImpl extends BaseComponentManager {
  private static final Logger LOG = Logger.getInstance(PlatformComponentManagerImpl.class);

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

  @Override
  protected Object initProgressIndicatorForLazyServices() {
    ProgressIndicator progressIndicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
    if (progressIndicator != null) {
      progressIndicator.setIndeterminate(false);
      progressIndicator.setFraction(0);
    }
    return progressIndicator;
  }

  @Override
  protected void checkCanceledAndChangeProgress(@Nullable Object p, int pos, int maxPos) {
    ProgressIndicator progressIndicator = (ProgressIndicator)p;
    if (progressIndicator != null) {
      progressIndicator.checkCanceled();

      progressIndicator.setFraction(pos / (float)maxPos);
    }
    else {
      checkCanceled();
    }
  }

  @Override
  protected boolean logSlowComponents() {
    return LOG.isDebugEnabled() || ApplicationProperties.isInSandbox();
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
    if (provider != null) {
      provider.checkForCanceled();
    }
  }

  @Nullable
  protected ProgressIndicatorProvider getProgressIndicatorProvider() {
    if (myParent instanceof PlatformComponentManagerImpl) {
      return ((PlatformComponentManagerImpl)myParent).getProgressIndicatorProvider();
    }
    return null;
  }

  @Nullable
  protected IComponentStore getStateStore() {
    return null;
  }

  @Override
  protected <T> T runServiceInitialize(@Nonnull InjectingBinding descriptor, @Nonnull Supplier<T> runnable) {
    // prevent storages from flushing and blocking FS
    try (AccessToken ignored = HeavyProcessLatch.INSTANCE.processStarted("Creating component '" + descriptor.getImplClassName() + "'")) {
      return super.runServiceInitialize(descriptor, runnable);
    }
  }
}
