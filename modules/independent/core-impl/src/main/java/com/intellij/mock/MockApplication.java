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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.impl.ModalityStateEx;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ThrowableComputable;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import consulo.annotations.RequiredWriteAction;
import org.jetbrains.ide.PooledThreadExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class MockApplication extends MockComponentManager implements Application {
  private ModalityState MODALITY_STATE_NONE;

  public static int INSTANCES_CREATED = 0;

  public MockApplication(@Nonnull Disposable parentDisposable) {
    super(parentDisposable);
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
  public boolean isCompilerServerMode() {
    return false;
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
  public void restart() {
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

  @RequiredDispatchThread
  @Override
  public void runWriteAction(@Nonnull Runnable action) {
    action.run();
  }

  @RequiredDispatchThread
  @Override
  public <T> T runWriteAction(@Nonnull Computable<T> computation) {
    return computation.compute();
  }

  @RequiredDispatchThread
  @Override
  public <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    return computation.compute();
  }

  @Nonnull
  @Override
  public AccessToken acquireReadActionLock() {
    return AccessToken.EMPTY_ACCESS_TOKEN;
  }

  @RequiredDispatchThread
  @Nonnull
  @Override
  public AccessToken acquireWriteActionLock(@Nonnull Class marker) {
    return AccessToken.EMPTY_ACCESS_TOKEN;
  }

  @RequiredDispatchThread
  @Override
  public boolean hasWriteAction(@Nullable Class<?> actionClass) {
    return false;
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

  @RequiredDispatchThread
  @Override
  public long getIdleTime() {
    return 0;
  }

  @Nonnull
  @Override
  public ModalityState getNoneModalityState() {
    if (MODALITY_STATE_NONE == null) {
      MODALITY_STATE_NONE = new ModalityStateEx() {
        @Override
        public boolean dominates(@Nonnull ModalityState anotherState) {
          return false;
        }

        @Override
        public String toString() {
          return "NONE";
        }
      };
    }
    return MODALITY_STATE_NONE;
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

  @RequiredDispatchThread
  @Override
  public void saveAll() {
  }

  @Override
  public void saveSettings() {
  }
}
