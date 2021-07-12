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
package com.intellij.mock;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationListener;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ThrowableComputable;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.ide.PooledThreadExecutor;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Deprecated
public class MockApplication extends MockComponentManager implements Application {
  public static int INSTANCES_CREATED = 0;

  public MockApplication(@Nonnull Disposable parentDisposable) {
    super(null, parentDisposable);
    INSTANCES_CREATED++;
  }

  @Override
  public boolean isInternal() {
    return false;
  }

  @Override
  public boolean isDispatchThread() {
    return true;
  }

  @Override
  public boolean isWriteThread() {
    return true;
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Nonnull
  @Override
  public consulo.ui.image.Image getIcon() {
    throw new IllegalArgumentException();
  }

  @Nonnull
  @Override
  public UIAccess getLastUIAccess() {
    return null;
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
  public boolean isReadAccessAllowed() {
    return true;
  }

  @Override
  public boolean isWriteAccessAllowed() {
    return true;
  }

  @Override
  public boolean isHeadlessEnvironment() {
    return true;
  }

  @Override
  public boolean isCommandLine() {
    return true;
  }

  @Nonnull
  @Override
  public Future<?> executeOnPooledThread(@Nonnull Runnable action) {
    return PooledThreadExecutor.INSTANCE.submit(action);
  }

  @Nonnull
  @Override
  public <T> Future<T> executeOnPooledThread(@Nonnull Callable<T> action) {
    return PooledThreadExecutor.INSTANCE.submit(action);
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
  public void runReadAction(@Nonnull Runnable action) {
    action.run();
  }

  @Override
  public <T> T runReadAction(@Nonnull Computable<T> computation) {
    return computation.compute();
  }

  @Override
  public <T, E extends Throwable> T runReadAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    return computation.compute();
  }

  @RequiredUIAccess
  @Override
  public void runWriteAction(@Nonnull Runnable action) {
    action.run();
  }

  @RequiredUIAccess
  @Override
  public <T> T runWriteAction(@Nonnull Computable<T> computation) {
    return computation.compute();
  }

  @RequiredUIAccess
  @Override
  public <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    return computation.compute();
  }

  @Override
  public boolean hasWriteAction(@Nonnull Class<?> actionClass) {
    return false;
  }

  @Nonnull
  @Override
  public AccessToken acquireReadActionLock() {
    return AccessToken.EMPTY_ACCESS_TOKEN;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AccessToken acquireWriteActionLock(@Nonnull Class marker) {
    return AccessToken.EMPTY_ACCESS_TOKEN;
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

  @Override
  public long getStartTime() {
    return 0;
  }

  @RequiredUIAccess
  @Override
  public long getIdleTime() {
    return 0;
  }

  @Nonnull
  @Override
  public ModalityState getNoneModalityState() {
    return ModalityState.NON_MODAL;
  }

  @Override
  public void invokeLater(@Nonnull final Runnable runnable, @Nonnull final Condition expired) {
  }

  @Override
  public void invokeLater(@Nonnull final Runnable runnable, @Nonnull final ModalityState state, @Nonnull final Condition expired) {
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable) {
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
  }

  @Nonnull
  @Override
  public ModalityState getCurrentModalityState() {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public ModalityState getAnyModalityState() {
    return getNoneModalityState();
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

  @Override
  public void exit() {
  }

  @RequiredUIAccess
  @Override
  public void saveAll() {
  }

  @Override
  public void saveSettings() {
  }
}
