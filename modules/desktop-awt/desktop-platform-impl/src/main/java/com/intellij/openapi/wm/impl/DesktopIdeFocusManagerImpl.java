/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.ExpirableRunnable;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WeakFocusStackManager;
import consulo.wm.ProjectIdeFocusManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Singleton;
import javax.swing.*;
import java.awt.*;

@Singleton
public class DesktopIdeFocusManagerImpl implements ProjectIdeFocusManager {
  public DesktopIdeFocusManagerImpl() {
    WeakFocusStackManager.getInstance();
  }

  @Override
  @Nonnull
  public AsyncResult<Void> requestFocus(@Nonnull final Component c, final boolean forced) {
    return IdeFocusManager.getGlobalInstance().requestFocus(c, forced);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> requestFocus(@Nonnull consulo.ui.Component c, boolean forced) {
    return IdeFocusManager.getGlobalInstance().requestFocus(c, forced);
  }

  @Override
  public AsyncResult<Void> requestFocusInProject(@Nonnull Component c, @Nullable Project project) {
    return IdeFocusManager.getGlobalInstance().requestFocusInProject(c, project);
  }

  @Override
  public JComponent getFocusTargetFor(@Nonnull final JComponent comp) {
    return IdeFocusManager.getGlobalInstance().getFocusTargetFor(comp);
  }

  @Override
  public void doWhenFocusSettlesDown(@Nonnull final Runnable runnable) {
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(runnable);
  }

  @Override
  public void doWhenFocusSettlesDown(@Nonnull Runnable runnable, @Nonnull ModalityState modality) {
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(runnable, modality);
  }

  @Override
  public void doWhenFocusSettlesDown(@Nonnull ExpirableRunnable runnable) {
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(runnable);
  }

  @Override
  @Nullable
  public Component getFocusedDescendantFor(@Nonnull final Component comp) {
    return IdeFocusManager.getGlobalInstance().getFocusedDescendantFor(comp);
  }

  @Override
  public void typeAheadUntil(@Nonnull ActionCallback callback, @Nonnull String cause) {
    IdeFocusManager.getGlobalInstance().typeAheadUntil(callback, cause);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> requestDefaultFocus(boolean forced) {
    //todo need to implement
    return AsyncResult.resolved();
  }

  @Override
  public boolean isFocusTransferEnabled() {
    return IdeFocusManager.getGlobalInstance().isFocusTransferEnabled();
  }

  @Override
  public void setTypeaheadEnabled(boolean enabled) {
    IdeFocusManager.getGlobalInstance().setTypeaheadEnabled(enabled);
  }

  @Override
  public Component getFocusOwner() {
    return IdeFocusManager.getGlobalInstance().getFocusOwner();
  }

  @Override
  public void runOnOwnContext(@Nonnull DataContext context, @Nonnull Runnable runnable) {
    IdeFocusManager.getGlobalInstance().runOnOwnContext(context, runnable);
  }

  @Override
  public Component getLastFocusedFor(IdeFrame frame) {
    return IdeFocusManager.getGlobalInstance().getLastFocusedFor(frame);
  }

  @Override
  public IdeFrame getLastFocusedFrame() {
    return IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
  }

  @Override
  public void toFront(JComponent c) {
    IdeFocusManager.getGlobalInstance().toFront(c);
  }

  @Override
  public void dispose() {
  }
}
