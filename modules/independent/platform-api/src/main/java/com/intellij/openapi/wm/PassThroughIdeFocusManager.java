/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.wm;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Expirable;
import com.intellij.openapi.util.ExpirableRunnable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class PassThroughIdeFocusManager extends IdeFocusManager {

  private static final PassThroughIdeFocusManager ourInstance = new PassThroughIdeFocusManager();

  public static PassThroughIdeFocusManager getInstance() {
    return ourInstance;
  }

  @Override
  @Nonnull
  public AsyncResult<Void> requestFocus(@Nonnull Component c, boolean forced) {
    c.requestFocus();
    return AsyncResult.done(null);
  }

  @Override
  @Nonnull
  public AsyncResult<Void> requestFocus(@Nonnull FocusCommand command, boolean forced) {
    return command.run();
  }

  @Override
  public JComponent getFocusTargetFor(@Nonnull JComponent comp) {
    return comp;
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
  public Component getFocusedDescendantFor(Component comp) {
    final Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focused == null) return null;

    if (focused == comp || SwingUtilities.isDescendingFrom(focused, comp)) return focused;

    return null;
  }

  @Override
  public boolean dispatch(@Nonnull KeyEvent e) {
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
    return new Expirable() {
      @Override
      public boolean isExpired() {
        return false;
      }
    };
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
    return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
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
  public boolean isFocusBeingTransferred() {
    return false;
  }

  @Override
  public void dispose() {
  }
}
