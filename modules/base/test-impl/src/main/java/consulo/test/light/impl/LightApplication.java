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

import com.intellij.openapi.application.*;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.extensions.impl.ExtensionAreaId;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.containers.MultiMap;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.container.plugin.PluginListenerDescriptor;
import consulo.disposer.Disposable;
import consulo.injecting.InjectingContainer;
import consulo.injecting.InjectingContainerBuilder;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightApplication extends ComponentManagerImpl implements Application {
  private final Disposable myLastDisposable;
  private final LightExtensionRegistrator myRegistrator;

  public LightApplication(Disposable lastDisposable, LightExtensionRegistrator registrator) {
    super(null, "LightApplication", ExtensionAreaId.APPLICATION, false);
    myLastDisposable = lastDisposable;
    myRegistrator = registrator;

    ApplicationManager.setApplication(this, myLastDisposable);

    buildInjectingContainer();
  }

  @Override
  protected void fillListenerDescriptors(MultiMap<String, PluginListenerDescriptor> mapByTopic) {
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
  protected void registerExtensionPointsAndExtensions(ExtensionsAreaImpl area) {
    myRegistrator.registerExtensionPointsAndExtensions(area);
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
  public <T> T runReadAction(@Nonnull Computable<T> computation) {
    return computation.compute();
  }

  @RequiredUIAccess
  @Override
  public void runWriteAction(@Nonnull Runnable action) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public <T> T runWriteAction(@Nonnull Computable<T> computation) {
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
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull Condition expired) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull Condition expired) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public ModalityState getCurrentModalityState() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public ModalityState getModalityStateForComponent(@Nonnull Component c) {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public ModalityState getDefaultModalityState() {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public ModalityState getNoneModalityState() {
    return ModalityState.NON_MODAL;
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
  public <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasWriteAction(@Nonnull Class<?> actionClass) {
    return false;
  }

  @Override
  public <T, E extends Throwable> T runReadAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    return computation.compute();
  }
}
