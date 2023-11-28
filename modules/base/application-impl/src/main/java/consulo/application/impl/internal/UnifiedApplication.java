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
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 12-Jul-22
 */
public abstract class UnifiedApplication extends BaseApplication {
  public UnifiedApplication(@Nonnull ComponentBinding componentBinding, @Nonnull SimpleReference<? extends StartupProgress> splashRef) {
    super(componentBinding, splashRef);

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
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
    UIAccess lastUIAccess = getLastUIAccess();

    lastUIAccess.give(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull BooleanSupplier expired) {
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
    return ModalityState.nonModal();
  }

  @Nonnull
  @Override
  public ModalityState getModalityStateForComponent(@Nonnull Component c) {
    return ModalityState.nonModal();
  }

  @Nonnull
  @Override
  public ModalityState getDefaultModalityState() {
    return ModalityState.nonModal();
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
