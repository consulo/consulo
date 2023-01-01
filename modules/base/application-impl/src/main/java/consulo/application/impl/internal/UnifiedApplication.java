/*
 * Copyright 2013-2022 consulo.io
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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentProfiles;
import consulo.application.ApplicationManager;
import consulo.application.TransactionGuard;
import consulo.application.impl.internal.start.StartupProgress;
import consulo.component.ComponentManager;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 12-Jul-22
 */
public abstract class UnifiedApplication extends BaseApplication {
  public UnifiedApplication(@Nonnull SimpleReference<? extends StartupProgress> splashRef) {
    super(splashRef);

    myLock = new ReadMostlyRWLock(null);

    ApplicationManager.setApplication(this);
  }

  @Override
  public int getProfiles() {
    return super.getProfiles() | ComponentProfiles.UNIFIED;
  }

  @Override
  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
    super.bootstrapInjectingContainer(builder);

    builder.bind(TransactionGuard.class).to(new UnifiedTransactionGuardImpl());
  }

  @Override
  public void invokeLaterOnWriteThread(@Nonnull Runnable action, @Nonnull ModalityState modal, @Nonnull BooleanSupplier expired) {
    UIAccess uiAccess = getLastUIAccess();
    uiAccess.give(() -> runIntendedWriteActionOnCurrentThread(action));
  }

  @Override
  public void exit(boolean force, boolean exitConfirmed) {

  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {
    if (!isReadAccessAllowed()) {
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
  public void invokeLater(@Nonnull Runnable runnable) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.give(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull BooleanSupplier expired) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.give(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull consulo.ui.ModalityState state) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.give(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull consulo.ui.ModalityState state, @Nonnull BooleanSupplier expired) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.give(runnable);
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull consulo.ui.ModalityState modalityState) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.giveAndWait(runnable);
  }

  @Nonnull
  @Override
  public IdeaModalityState getCurrentModalityState() {
    return IdeaModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public IdeaModalityState getModalityStateForComponent(@Nonnull Component c) {
    return IdeaModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public IdeaModalityState getDefaultModalityState() {
    return IdeaModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public IdeaModalityState getNoneModalityState() {
    return IdeaModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public IdeaModalityState getAnyModalityState() {
    return IdeaModalityState.NON_MODAL;
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
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                     @Nonnull String progressTitle,
                                                     boolean canBeCanceled,
                                                     boolean shouldShowModalWindow,
                                                     @Nullable ComponentManager project,
                                                     @Nullable JComponent parentComponent,
                                                     @Nullable @Nls(capitalization = Nls.Capitalization.Title) String cancelText) {
    return true;
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public void assertTimeConsuming() {

  }

  @Override
  public boolean isInternal() {
    return true;
  }

  @Override
  public boolean isUnifiedApplication() {
    return true;
  }
}
