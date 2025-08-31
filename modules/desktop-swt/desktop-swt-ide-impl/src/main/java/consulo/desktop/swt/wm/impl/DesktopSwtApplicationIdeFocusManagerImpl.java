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
package consulo.desktop.swt.wm.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.component.ComponentManager;
import consulo.dataContext.DataContext;
import consulo.ui.ModalityState;
import consulo.util.concurrent.AsyncResult;
import consulo.application.ui.wm.ExpirableRunnable;
import consulo.project.ui.wm.IdeFrame;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 11/12/2021
 */
@Singleton
@ServiceImpl
public class DesktopSwtApplicationIdeFocusManagerImpl implements ApplicationIdeFocusManager {
  @Override
  @Nonnull
  public AsyncResult<Void> requestFocus(@Nonnull Component c, boolean forced) {
    return AsyncResult.resolved();
  }

  @Nonnull
  @Override
  public AsyncResult<Void> requestFocus(@Nonnull consulo.ui.Component c, boolean forced) {
    return AsyncResult.resolved();
  }

  @Override
  public JComponent getFocusTargetFor(@Nonnull JComponent comp) {
    return null;
  }

  @Override
  public void doWhenFocusSettlesDown(@Nonnull Runnable runnable) {
    runnable.run();
  }

  @Override
  public void doWhenFocusSettlesDown(@Nonnull Runnable runnable, @Nonnull ModalityState modality) {
    runnable.run();
  }

  @Override
  public void doWhenFocusSettlesDown(@Nonnull ExpirableRunnable runnable) {
    if (!runnable.isExpired()) {
      runnable.run();
    }
  }

  @Override
  public Component getFocusedDescendantFor(Component c) {
    return null;
  }

  @Override
  @Nonnull
  public AsyncResult<Void> requestDefaultFocus(boolean forced) {
    return AsyncResult.resolved();
  }

  @Override
  public boolean isFocusTransferEnabled() {
    return true;
  }

  @Override
  public Component getFocusOwner() {
    return null;
  }

  @Override
  public void runOnOwnContext(@Nonnull DataContext context, @Nonnull Runnable runnable) {
    runnable.run();
  }

  @Override
  public Component getLastFocusedFor(FocusableFrame frame) {
    return null;
  }

  @Override
  public IdeFrame getLastFocusedFrame() {
    return null;
  }

  @Override
  public void toFront(JComponent c) {
  }

  @Override
  public void dispose() {
  }

  @Nonnull
  @Override
  public IdeFocusManager findInstanceByComponent(@Nonnull Component c) {
    return this;
  }

  @Nonnull
  @Override
  public IdeFocusManager findInstanceByContext(@Nullable DataContext context) {
    return this;
  }

  @Nonnull
  @Override
  public IdeFocusManager getInstanceForProject(@Nullable ComponentManager componentManager) {
    return this;
  }
}
