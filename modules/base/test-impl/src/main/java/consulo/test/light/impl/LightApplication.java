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
import consulo.annotation.component.ComponentScope;
import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.event.ApplicationListener;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.component.bind.InjectingBinding;
import consulo.component.impl.internal.BaseComponentManager;
import consulo.disposer.Disposable;
import consulo.component.internal.inject.InjectingContainer;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.MultiMap;
import consulo.util.lang.function.ThrowableSupplier;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightApplication extends BaseComponentManager implements Application {
  private final Disposable myLastDisposable;
  private final LightExtensionRegistrator myRegistrator;

  public LightApplication(Disposable lastDisposable, LightExtensionRegistrator registrator) {
    super(null, "LightApplication", ComponentScope.APPLICATION, false);
    myLastDisposable = lastDisposable;
    myRegistrator = registrator;

    ApplicationManager.setApplication(this, myLastDisposable);

    buildInjectingContainer();
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
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull consulo.ui.ModalityState state) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull consulo.ui.ModalityState state, @Nonnull BooleanSupplier expired) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull consulo.ui.ModalityState modalityState) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public IdeaModalityState getCurrentModalityState() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public IdeaModalityState getModalityStateForComponent(@Nonnull Component c) {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public IdeaModalityState getDefaultModalityState() {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public IdeaModalityState getNoneModalityState() {
    return IdeaModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public IdeaModalityState getAnyModalityState() {
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
