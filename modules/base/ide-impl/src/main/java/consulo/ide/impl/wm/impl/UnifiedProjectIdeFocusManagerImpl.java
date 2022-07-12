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
package consulo.ide.impl.wm.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.wm.ExpirableRunnable;
import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.component.ComponentManager;
import consulo.dataContext.DataContext;
import consulo.project.ui.wm.internal.ProjectIdeFocusManager;
import consulo.ui.ModalityState;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedProjectIdeFocusManagerImpl implements ProjectIdeFocusManager {
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
  public AsyncResult<Void> requestFocusInProject(@Nonnull Component c, @Nullable ComponentManager project) {
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
    return AsyncResult.resolved(null);
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
  public Component getLastFocusedFor(FocusableFrame frame) {
    return IdeFocusManager.getGlobalInstance().getLastFocusedFor(frame);
  }

  @Override
  public FocusableFrame getLastFocusedFrame() {
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
