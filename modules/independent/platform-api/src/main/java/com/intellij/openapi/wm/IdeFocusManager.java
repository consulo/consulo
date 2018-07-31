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
package com.intellij.openapi.wm;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Expirable;
import com.intellij.openapi.util.ExpirableRunnable;
import com.intellij.util.ui.UIUtil;
import consulo.platform.api.wm.ApplicationIdeFocusManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * This class receives focus requests, manages the, and delegates to the awt focus subsystem. All focus requests
 * should be done through this class. For example, to request focus on a component:
 * <pre>
 *   IdeFocusManager.getInstance(project).requestFocus(comp, true);
 * </pre>
 * This is the preferred way to request focus on components to
 * <pre>
 *   comp.requestFocus();
 * </pre>
 * <p>
 * This class is also responsible for delivering key events while focus transferring is in progress.
 * <p>
 * <code>IdeFocusManager</code> instance can be received per project or the global instance. The preferred way is
 * to use instance <code>IdeFocusManager.getInstance(project)</code>. If no project instance is available, then
 * <code>IdeFocusManager.getGlobalInstance()</code> can be used.
 */

public interface IdeFocusManager extends FocusRequestor {

  default AsyncResult<Void> requestFocusInProject(@Nonnull Component c, @Nullable Project project) {
    return requestFocus(c, false);
  }

  /**
   * Finds most suitable component to request focus to. For instance you may pass a JPanel instance,
   * this method will traverse into it's children to find focusable component
   *
   * @return suitable component to focus
   */
  @Nullable
  JComponent getFocusTargetFor(@Nonnull final JComponent comp);


  /**
   * Executes given runnable after all focus activities are finished
   */
  void doWhenFocusSettlesDown(@Nonnull Runnable runnable);

  default void doForceFocusWhenFocusSettlesDown(@Nonnull Component component) {
    doWhenFocusSettlesDown(() -> requestFocus(component, true));
  }

  /**
   * Executes given runnable after all focus activities are finished, immediately or later with the given modaliy state
   */
  void doWhenFocusSettlesDown(@Nonnull Runnable runnable, @Nonnull ModalityState modality);

  /**
   * Executes given runnable after all focus activities are finished
   */
  void doWhenFocusSettlesDown(@Nonnull ExpirableRunnable runnable);


  /**
   * Finds focused component among descendants of the given component. Descendants may be in child popups and windows
   */
  @Nullable
  Component getFocusedDescendantFor(final Component comp);

  /**
   * Dispatches given key event. This methods should not be called by the user code
   *
   * @return true is the event was dispatched, false - otherwise.
   */
  boolean dispatch(@Nonnull KeyEvent e);

  @Deprecated
  // use #typeAheadUntil(ActionCallback, String) instead
  default void typeAheadUntil(AsyncResult<Void> done) {
    typeAheadUntil(done, "No cause has been provided");
  }

  /**
   * Aggregates all key events until given callback object is processed
   *
   * @param done action callback
   */
  default void typeAheadUntil(ActionCallback done, @Nonnull String cause) {
  }

  /**
   * Reports if any focus activity is being done
   */
  boolean isFocusBeingTransferred();

  /**
   * Requests default focus. The method should not be called by the user code.
   */
  @Nonnull
  AsyncResult<Void> requestDefaultFocus(boolean forced);

  /**
   * Reports of focus transfer is enabled right now. It can be disabled if app is inactive. In this case
   * all focus requests will be either postponed or executed only if <code>FocusCommand</code> can be executed on an inaactive app.
   *
   * @see com.intellij.openapi.wm.FocusCommand#canExecuteOnInactiveApp()
   */
  boolean isFocusTransferEnabled();

  /**
   * Returns <code>Expirable</code> instance for the given counter of focus commands. As any new <code>FocusCommand</code>
   * is emitted to execute, the counter increments thus making the returned <code>Expirable</code> objects expired.
   */
  @Nonnull
  Expirable getTimestamp(boolean trackOnlyForcedCommands);

  /**
   * Returns <code>FocusRequestor</code> object which will emit focus requests unless expired.
   *
   * @see #getTimestamp(boolean)
   */
  @Nonnull
  FocusRequestor getFurtherRequestor();

  /**
   * Injects some procedure that will maybe do something with focus after all focus requests are fulfilled and
   * before focus transfer is reported ready.
   */
  void revalidateFocus(@Nonnull ExpirableRunnable runnable);

  /**
   * Enables or disables typeahead
   *
   * @see #typeAheadUntil(AsyncResult)
   */
  void setTypeaheadEnabled(boolean enabled);

  /**
   * Computes effective focus owner
   */
  Component getFocusOwner();

  /**
   * Runs runnable for which <code>DataContext</code> will no be computed from the current focus owner,
   * but used the given one
   */
  void runOnOwnContext(@Nonnull DataContext context, @Nonnull Runnable runnable);

  /**
   * Returns last focused component for the given <code>IdeFrame</code>
   */
  @Nullable
  Component getLastFocusedFor(@Nullable IdeFrame frame);

  /**
   * Returns last focused <code>IdeFrame</code>
   */
  @Nullable
  IdeFrame getLastFocusedFrame();

  /**
   * Put the container window to front. May not execute of the app is inactive or under some other conditions. This
   * is the preferred way to finding the container window and unconditionally calling <code>window.toFront()</code>
   */
  void toFront(JComponent c);

  default boolean isUnforcedRequestAllowed() {
    return false;
  }

  static IdeFocusManager getInstance(@Nullable Project project) {
    if (project == null || project.isDisposed() || !project.isInitialized()) return getGlobalInstance();

    return project.getComponent(IdeFocusManager.class);
  }

  @Nonnull
  static IdeFocusManager findInstanceByContext(@Nullable DataContext context) {
    IdeFocusManager instance = null;
    if (context != null) {
      instance = getInstanceSafe(context.getData(CommonDataKeys.PROJECT));
    }

    if (instance == null) {
      instance = findByComponent(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow());
    }

    if (instance == null) {
      instance = getGlobalInstance();
    }

    return instance;
  }

  @Nonnull
  static IdeFocusManager findInstanceByComponent(@Nonnull Component c) {
    final IdeFocusManager instance = findByComponent(c);
    return instance != null ? instance : findInstanceByContext(null);
  }


  @Nullable
  static IdeFocusManager findByComponent(Component c) {
    final Component parent = UIUtil.findUltimateParent(c);
    if (parent instanceof IdeFrame) {
      return getInstanceSafe(((IdeFrame)parent).getProject());
    }
    return null;
  }


  @Nullable
  static IdeFocusManager getInstanceSafe(@Nullable Project project) {
    if (project != null && !project.isDisposed() && project.isInitialized()) {
      return getInstance(project);
    }
    return null;
  }

  @Nonnull
  static IdeFocusManager findInstance() {
    final Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    return owner != null ? findInstanceByComponent(owner) : findInstanceByContext(null);
  }

  @Nonnull
  static IdeFocusManager getGlobalInstance() {
    IdeFocusManager fm = null;

    Application app = ApplicationManager.getApplication();
    if (app != null && app.hasComponent(ApplicationIdeFocusManager.class)) {
      fm = app.getComponent(ApplicationIdeFocusManager.class);
    }

    if (fm == null) {
      // happens when app is semi-initialized (e.g. when IDEA server dialog is shown)
      fm = PassThroughIdeFocusManager.getInstance();
    }

    return fm;
  }

}
