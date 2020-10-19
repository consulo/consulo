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
package consulo.web.wm.impl;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.ExpirableRunnable;
import com.intellij.openapi.wm.IdeFrame;
import consulo.wm.ApplicationIdeFocusManager;

import javax.annotation.Nonnull;
import jakarta.inject.Singleton;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-08-24
 */
@Singleton
public class WebApplicationIdeFocusManagerImpl implements ApplicationIdeFocusManager {
  @Override
  @Nonnull
  public AsyncResult<Void> requestFocus(@Nonnull final Component c, final boolean forced) {
    return AsyncResult.resolved();
  }

  @Nonnull
  @Override
  public AsyncResult<Void> requestFocus(@Nonnull consulo.ui.Component c, boolean forced) {
    return AsyncResult.resolved();
  }

  @Override
  public JComponent getFocusTargetFor(@Nonnull final JComponent comp) {
    return null;
  }

  @Override
  public void doWhenFocusSettlesDown(@Nonnull final Runnable runnable) {
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
  public Component getFocusedDescendantFor(final Component c) {
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
  public void setTypeaheadEnabled(boolean enabled) {
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
  public Component getLastFocusedFor(IdeFrame frame) {
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
}
