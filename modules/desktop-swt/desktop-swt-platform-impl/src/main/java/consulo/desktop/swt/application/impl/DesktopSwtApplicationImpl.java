/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.application.impl;

import com.intellij.ide.StartupProgress;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.impl.BaseApplication;
import consulo.desktop.swt.ui.impl.DesktopSwtUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ref.SimpleReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtApplicationImpl extends BaseApplication {
  public DesktopSwtApplicationImpl(@Nonnull SimpleReference<? extends StartupProgress> splashRef) {
    super(splashRef);
    
    ApplicationManager.setApplication(this);
  }

  @Override
  public void exit(boolean force, boolean exitConfirmed) {

  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {
    if(!isReadAccessAllowed()) {
      throw new IllegalArgumentException();
    }
  }

  @RequiredUIAccess
  @Override
  public void assertIsDispatchThread() {
    if (!isDispatchThread()) {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public void exit() {

  }

  @Override
  public boolean isReadAccessAllowed() {
    if (isDispatchThread()) {
      return true;
    }
    return isWriteThread() || myLock.isReadLockedByThisThread();
  }

  @Override
  public boolean isDispatchThread() {
    return UIAccess.isUIThread();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.give(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull Condition expired) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.give(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.give(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull Condition expired) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.give(runnable);
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.giveAndWait(runnable);
  }

  @Nonnull
  @Override
  public ModalityState getCurrentModalityState() {
    return ModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public ModalityState getModalityStateForComponent(@Nonnull Component c) {
    return ModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public ModalityState getDefaultModalityState() {
    return ModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public ModalityState getNoneModalityState() {
    return ModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public ModalityState getAnyModalityState() {
    return ModalityState.NON_MODAL;
  }

  @RequiredUIAccess
  @Override
  public long getIdleTime() {
    return 0;
  }

  @Override
  public boolean isHeadlessEnvironment() {
    return false;
  }

  @Override
  public boolean isDisposeInProgress() {
    return false;
  }

  @Override
  public void restart(boolean exitConfirmed) {

  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Nonnull
  @Override
  public UIAccess getLastUIAccess() {
    return DesktopSwtUIAccess.INSTANCE;
  }

  @RequiredUIAccess
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                     @Nonnull String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project,
                                                     JComponent parentComponent,
                                                     String cancelText) {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void assertIsDispatchThread(@Nullable JComponent component) {

  }

  @Override
  public void assertTimeConsuming() {

  }
}
