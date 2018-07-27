/*
 * Copyright 2013-2017 consulo.io
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
import com.intellij.openapi.util.Expirable;
import com.intellij.openapi.util.ExpirableRunnable;
import com.intellij.openapi.wm.FocusCommand;
import com.intellij.openapi.wm.FocusRequestor;
import com.intellij.openapi.wm.IdeFrame;
import consulo.platform.api.wp.ApplicationIdeFocusManager;
import consulo.platform.api.wp.ProjectIdeFocusManager;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public class WebIdeFocusManagerImpl implements ApplicationIdeFocusManager, ProjectIdeFocusManager {
  @Override
  @Nonnull
  public AsyncResult<Void> requestFocus(@Nonnull final Component c, final boolean forced) {
    return AsyncResult.done(null);
  }

  @Override
  @Nonnull
  public AsyncResult<Void> requestFocus(@Nonnull final FocusCommand command, final boolean forced) {
    command.run();
    return AsyncResult.done(null);
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
  public boolean dispatch(@Nonnull KeyEvent e) {
    return false;
  }

  @Override
  public boolean isFocusBeingTransferred() {
    return false;
  }

  @Override
  @Nonnull
  public AsyncResult<Void> requestDefaultFocus(boolean forced) {
    return AsyncResult.done(null);
  }

  @Override
  public boolean isFocusTransferEnabled() {
    return true;
  }

  @Nonnull
  @Override
  public Expirable getTimestamp(boolean trackOnlyForcedCommands) {
    return () -> false;
  }

  @Nonnull
  @Override
  public FocusRequestor getFurtherRequestor() {
    return this;
  }

  @Override
  public void revalidateFocus(@Nonnull ExpirableRunnable runnable) {
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
