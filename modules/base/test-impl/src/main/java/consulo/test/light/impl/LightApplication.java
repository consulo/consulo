/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ComponentScope;
import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.event.ApplicationListener;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.component.bind.InjectingBinding;
import consulo.component.impl.internal.BaseComponentManager;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainer;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.disposer.Disposable;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.MultiMap;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightApplication extends BaseComponentManager implements Application {
  private final ComponentBinding myComponentBinding;
  private final LightExtensionRegistrator myRegistrator;

  private final ProgressManager myProgressManager;

  public LightApplication(Disposable lastDisposable, ComponentBinding componentBinding, LightExtensionRegistrator registrator) {
    super(null,
          "LightApplication",
          ComponentScope.APPLICATION,
          componentBinding,
          false);
    myComponentBinding = componentBinding;
    myRegistrator = registrator;

    myProgressManager = new LightProgressManager();
    
    ApplicationManager.setApplication(this, lastDisposable);

    buildInjectingContainer();
  }

  public ComponentBinding getComponentBinding() {
    return myComponentBinding;
  }

  @Nonnull
  @Override
  public ProgressManager getProgressManager() {
    return myProgressManager;
  }

  @Override
  public int getProfiles() {
    return ComponentProfiles.LIGHT_TEST;
  }

  @Override
  protected void fillListenerDescriptors(MultiMap<String, InjectingBinding> mapByTopic) {
  }

  @Nonnull
  @Override
  protected InjectingContainer findRootContainer() {
    return InjectingContainer.root(getClass().getClassLoader());
  }

  @Override
  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
    super.bootstrapInjectingContainer(builder);

    builder.bind(Application.class).to(this);
    builder.bind(FileTypeRegistry.class).to(new LightFileTypeRegistry());
    builder.bind(ProgressIndicatorProvider.class).to(myProgressManager);
  }

  @Override
  protected void registerServices(InjectingContainerBuilder builder) {
    myRegistrator.registerServices(builder);
  }

  @Override
  public void runReadAction(@Nonnull Runnable action) {
    action.run();
  }

  @Override
  public <T> T runReadAction(@Nonnull Supplier<T> computation) {
    return computation.get();
  }

  @Override
  public boolean tryRunReadAction(@Nonnull Runnable action) {
    action.run();
    return true;
  }

  @RequiredUIAccess
  @Override
  public void runWriteAction(@Nonnull Runnable action) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public <T> T runWriteAction(@Nonnull Supplier<T> computation) {
    throw new UnsupportedOperationException();
  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {

  }

  @RequiredWriteAction
  @Override
  public void assertWriteAccessAllowed() {

  }

  @RequiredUIAccess
  @Override
  public void assertIsDispatchThread() {

  }

  @Override
  public void addApplicationListener(@Nonnull ApplicationListener listener) {

  }

  @Override
  public void addApplicationListener(@Nonnull ApplicationListener listener, @Nonnull Disposable parent) {

  }

  @Override
  public void removeApplicationListener(@Nonnull ApplicationListener listener) {

  }

  @RequiredUIAccess
  @Override
  public void saveAll() {

  }

  @Override
  public void saveSettings() {

  }

  @Override
  public void exit() {

  }

  @Override
  public boolean isReadAccessAllowed() {
    return true;
  }

  @Override
  public boolean isDispatchThread() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isWriteThread() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull BooleanSupplier expired) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull BooleanSupplier expired) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public ModalityState getCurrentModalityState() {
    return ModalityState.nonModal();
  }

  @Nonnull
  @Override
  public ModalityState getDefaultModalityState() {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public ModalityState getNoneModalityState() {
    return ModalityState.nonModal();
  }

  @Nonnull
  @Override
  public ModalityState getAnyModalityState() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getStartTime() {
    return 0;
  }

  @RequiredUIAccess
  @Override
  public long getIdleTime() {
    return 0;
  }

  @Override
  public boolean isHeadlessEnvironment() {
    return true;
  }

  @Nonnull
  @Override
  public Future<?> executeOnPooledThread(@Nonnull Runnable action) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public <T> Future<T> executeOnPooledThread(@Nonnull Callable<T> action) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDisposeInProgress() {
    return false;
  }

  @Override
  public boolean isRestartCapable() {
    return false;
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public UIAccess getLastUIAccess() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public AccessToken acquireReadActionLock() {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AccessToken acquireWriteActionLock(@Nonnull Class marker) {
    return AccessToken.EMPTY_ACCESS_TOKEN;
  }

  @RequiredUIAccess
  @Override
  public <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableSupplier<T, E> computation) throws E {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasWriteAction(@Nonnull Class<?> actionClass) {
    return false;
  }

  @Override
  public <T, E extends Throwable> T runReadAction(@Nonnull ThrowableSupplier<T, E> computation) throws E {
    return computation.get();
  }
}
