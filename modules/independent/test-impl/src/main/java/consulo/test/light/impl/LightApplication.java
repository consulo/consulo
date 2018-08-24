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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.*;
import com.intellij.openapi.components.ExtensionAreas;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ThrowableComputable;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import consulo.annotations.RequiredWriteAction;
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

  public LightApplication(Disposable lastDisposable) {
    super(null, "LightApplication", ExtensionAreas.APPLICATION);
    myLastDisposable = lastDisposable;

    ApplicationManager.setApplication(this, myLastDisposable);

    // reset area
    Disposer.register(myLastDisposable, () -> Extensions.setRootArea(null));
  }


  @Override
  protected void maybeSetRootArea() {
    Extensions.setRootArea(getExtensionsArea());
  }

  @Override
  public void runReadAction(@Nonnull Runnable action) {

  }

  @Override
  public <T> T runReadAction(@Nonnull Computable<T> computation) {
    return null;
  }

  @RequiredDispatchThread
  @Override
  public void runWriteAction(@Nonnull Runnable action) {

  }

  @RequiredDispatchThread
  @Override
  public <T> T runWriteAction(@Nonnull Computable<T> computation) {
    return null;
  }

  @RequiredReadAction
  @Override
  public boolean hasWriteAction(@Nonnull Class<?> actionClass) {
    return false;
  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {

  }

  @RequiredWriteAction
  @Override
  public void assertWriteAccessAllowed() {

  }

  @RequiredDispatchThread
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

  @RequiredDispatchThread
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
    return false;
  }

  @Override
  public boolean isDispatchThread() {
    return false;
  }

  @Override
  public boolean isWriteThread() {
    return false;
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable) {

  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull Condition expired) {

  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {

  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull Condition expired) {

  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {

  }

  @Nonnull
  @Override
  public ModalityState getCurrentModalityState() {
    return null;
  }

  @Nonnull
  @Override
  public ModalityState getModalityStateForComponent(@Nonnull Component c) {
    return null;
  }

  @Nonnull
  @Override
  public ModalityState getDefaultModalityState() {
    return null;
  }

  @Nonnull
  @Override
  public ModalityState getNoneModalityState() {
    return null;
  }

  @Nonnull
  @Override
  public ModalityState getAnyModalityState() {
    return null;
  }

  @Override
  public long getStartTime() {
    return 0;
  }

  @RequiredDispatchThread
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
    return null;
  }

  @Nonnull
  @Override
  public <T> Future<T> executeOnPooledThread(@Nonnull Callable<T> action) {
    return null;
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
  public void restart() {

  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return null;
  }

  @Nonnull
  @Override
  public AccessToken acquireReadActionLock() {
    return null;
  }

  @RequiredDispatchThread
  @Nonnull
  @Override
  public AccessToken acquireWriteActionLock(@Nonnull Class marker) {
    return null;
  }

  @RequiredDispatchThread
  @Override
  public <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    return null;
  }

  @Override
  public <T, E extends Throwable> T runReadAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    return null;
  }
}
