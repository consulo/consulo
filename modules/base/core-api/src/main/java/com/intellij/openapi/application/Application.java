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
package com.intellij.openapi.application;

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ThrowableComputable;
import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;

import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Provides access to core application-wide functionality and methods for working with the IDE
 * thread model. The thread model defines two main types of actions which can access the PSI and other
 * IDE data structures: read actions (which do not modify the data) and write actions (which modify
 * some data).<p>
 * You can call methods requiring read access from the Swing event-dispatch thread without using
 * {@link #runReadAction} method. If you need to invoke such methods from another thread you have to use
 * {@link #runReadAction}. Multiple read actions can run at the same time without locking each other.
 * <p>
 * Write actions can be called only from the Swing thread using {@link #runWriteAction} method.
 * If there are read actions running at this moment <code>runWriteAction</code> is blocked until they are completed.
 */
public interface Application extends ComponentManager {
  @Nonnull
  public static Application get() {
    Application application = ApplicationManager.getApplication();
    if (application == null) {
      throw new IllegalArgumentException("Application is not initialized");
    }
    return application;
  }

  /**
   * Causes {@code runnable} to be executed asynchronously under Write Intent lock on some thread,
   * with {@link ModalityState#defaultModalityState()} modality state.
   *
   * @param action the runnable to execute.
   */
  default void invokeLaterOnWriteThread(@Nonnull Runnable action) {
    throw new AbstractMethodError();
  }

  /**
   * Causes {@code runnable} to be executed asynchronously under Write Intent lock on some thread,
   * when IDE is in the specified modality state (or a state with less modal dialogs open).
   *
   * @param action the runnable to execute.
   * @param modal  the state in which action will be executed
   */
  default void invokeLaterOnWriteThread(@Nonnull Runnable action, @Nonnull ModalityState modal) {
    throw new AbstractMethodError();
  }

  /**
   * Causes {@code runnable} to be executed asynchronously under Write Intent lock on some thread,
   * when IDE is in the specified modality state (or a state with less modal dialogs open)
   * - unless the expiration condition is fulfilled.
   *
   * @param action  the runnable to execute.
   * @param modal   the state in which action will be executed
   * @param expired condition to check before execution.
   */
  default void invokeLaterOnWriteThread(@Nonnull Runnable action, @Nonnull ModalityState modal, @Nonnull Condition<?> expired) {
    throw new AbstractMethodError();
  }

  /**
   * Runs the specified read action. Can be called from any thread. The action is executed immediately
   * if no write action is currently running, or blocked until the currently running write action completes.
   *
   * @param action the action to run.
   */
  void runReadAction(@Nonnull Runnable action);

  /**
   * Runs the specified computation in a read action. Can be called from any thread. The action is executed
   * immediately if no write action is currently running, or blocked until the currently running write action
   * completes.
   *
   * @param computation the computation to perform.
   * @return the result returned by the computation.
   */
  <T> T runReadAction(@Nonnull Computable<T> computation);

  /**
   * Runs the specified computation in a read action. Can be called from any thread. The action is executed
   * immediately if no write action is currently running, or blocked until the currently running write action
   * completes.
   *
   * @param computation the computation to perform.
   * @return the result returned by the computation.
   * @throws E re-frown from ThrowableComputable
   */
  <T, E extends Throwable> T runReadAction(@Nonnull ThrowableComputable<T, E> computation) throws E;

  /**
   * Runs the specified write action. Must be called from the Swing dispatch thread. The action is executed
   * immediately if no read actions are currently running, or blocked until all read actions complete.
   *
   * @param action the action to run
   */
  @RequiredUIAccess
  void runWriteAction(@Nonnull Runnable action);

  /**
   * Runs the specified computation in a write action. Must be called from the Swing dispatch thread.
   * The action is executed immediately if no read actions or write actions are currently running,
   * or blocked until all read actions and write actions complete.
   *
   * @param computation the computation to run
   * @return the result returned by the computation.
   */
  @RequiredUIAccess
  <T> T runWriteAction(@Nonnull Computable<T> computation);

  /**
   * Runs the specified computation in a write action. Must be called from the Swing dispatch thread.
   * The action is executed immediately if no read actions or write actions are currently running,
   * or blocked until all read actions and write actions complete.
   *
   * @param computation the computation to run
   * @return the result returned by the computation.
   * @throws E re-frown from ThrowableComputable
   */
  @RequiredUIAccess
  <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableComputable<T, E> computation) throws E;

  /**
   * Returns {@code true} if there is currently executing write action of the specified class.
   *
   * @param actionClass the class of the write action to return.
   * @return {@code true} if the action is running, or {@code false} if no action of the specified class is currently executing.
   */
  boolean hasWriteAction(@Nonnull Class<?> actionClass);

  /**
   * Asserts whether the read access is allowed.
   */
  @RequiredReadAction
  void assertReadAccessAllowed();

  /**
   * Asserts whether the write access is allowed.
   */
  @RequiredWriteAction
  void assertWriteAccessAllowed();

  /**
   * Asserts whether the method is being called from the event dispatch thread.
   */
  @RequiredUIAccess
  void assertIsDispatchThread();

  /**
   * Asserts whether the method is being called from the write thread.
   */
  default void assertIsWriteThread() {
    assertIsDispatchThread();
  }

  /**
   * Adds an {@link ApplicationListener}.
   *
   * @param listener the listener to add
   */
  void addApplicationListener(@Nonnull ApplicationListener listener);

  /**
   * Adds an {@link ApplicationListener}.
   *
   * @param listener the listener to add
   * @param parent   the parent disposable which dispose will trigger this listener removal
   */
  void addApplicationListener(@Nonnull ApplicationListener listener, @Nonnull Disposable parent);

  /**
   * Removes an {@link ApplicationListener}.
   *
   * @param listener the listener to remove
   */
  void removeApplicationListener(@Nonnull ApplicationListener listener);

  /**
   * Saves all open documents and projects.
   */
  @RequiredUIAccess
  void saveAll();

  /**
   * Saves all application settings.
   */
  void saveSettings();

  /**
   * Exits the application, showing the exit confirmation prompt if it is enabled.
   */
  void exit();

  /**
   * Checks if the write access is currently allowed.
   *
   * @return true if the write access is currently allowed, false otherwise.
   * @see #assertWriteAccessAllowed()
   * @see #runWriteAction(Runnable)
   */
  default boolean isWriteAccessAllowed() {
    return isWriteThread();
  }

  /**
   * Checks if the read access is currently allowed.
   *
   * @return true if the read access is currently allowed, false otherwise.
   * @see #assertReadAccessAllowed()
   * @see #runReadAction(Runnable)
   */
  boolean isReadAccessAllowed();

  /**
   * Checks if the current thread is the Swing dispatch thread.
   * <p>
   * Dispatch thread not always is "write thread". For checking if current thread is "write thread" use {@link #isWriteThread()}
   *
   * @return true if the current thread is the Swing dispatch thread, false otherwise.
   */
  boolean isDispatchThread();

  /**
   * Checks if the current thread is "write thread".
   *
   * @return true if the current thread is the "write thread", false otherwise.
   */
  boolean isWriteThread();

  /**
   * Causes {@code runnable.run()} to be executed asynchronously on the
   * AWT event dispatching thread. This will happen after all
   * pending AWT events have been processed.
   *
   * @param runnable the runnable to execute.
   */
  void invokeLater(@Nonnull Runnable runnable);

  /**
   * Causes {@code runnable.run()} to be executed asynchronously on the
   * AWT event dispatching thread - unless the expiration condition is fulfilled.
   * This will happen after all pending AWT events have been processed.
   *
   * @param runnable the runnable to execute.
   * @param expired  condition to check before execution.
   */
  void invokeLater(@Nonnull Runnable runnable, @Nonnull Condition expired);

  /**
   * Causes {@code runnable.run()} to be executed asynchronously on the
   * AWT event dispatching thread, when IDE is in the specified modality
   * state.
   *
   * @param runnable the runnable to execute.
   * @param state    the state in which the runnable will be executed.
   */
  void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state);

  /**
   * Same as {@link #invokeAndWait(Runnable, ModalityState)}, using {@link ModalityState#defaultModalityState()}.
   */
  default void invokeAndWait(@Nonnull Runnable runnable) throws ProcessCanceledException {
    invokeAndWait(runnable, ModalityState.defaultModalityState());
  }

  /**
   * Causes {@code runnable.run()} to be executed asynchronously on the
   * AWT event dispatching thread, when IDE is in the specified modality
   * state - unless the expiration condition is fulfilled.
   * This will happen after all pending AWT events have been processed.
   *
   * @param runnable the runnable to execute.
   * @param state    the state in which the runnable will be executed.
   * @param expired  condition to check before execution.
   */
  void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull Condition expired);

  /**
   * <p>Causes {@code runnable.run()} to be executed synchronously on the
   * AWT event dispatching thread, when IDE is in the specified modality
   * state. This call blocks until all pending AWT events have been processed and (then)
   * {@code runnable.run()} returns.</p>
   * <p>
   * <p>If current thread is an event dispatch thread then {@code runnable.run()}
   * is executed immediately.</p>
   *
   * @param runnable      the runnable to execute.
   * @param modalityState the state in which the runnable will be executed.
   */
  void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState);

  /**
   * Returns the current modality state for the Swing dispatch thread.
   *
   * @return the current modality state.
   */
  @Nonnull
  ModalityState getCurrentModalityState();

  /**
   * Returns the modality state for the dialog to which the specified component belongs.
   *
   * @param c the component for which the modality state is requested.
   * @return the modality state.
   */
  @Nonnull
  ModalityState getModalityStateForComponent(@Nonnull Component c);

  /**
   * Returns the current modality state for the current thread (which may be different
   * from the Swing dispatch thread).
   *
   * @return the modality state for the current thread.
   */
  @Nonnull
  ModalityState getDefaultModalityState();

  /**
   * Returns the modality state representing the state when no modal dialogs
   * are active.
   *
   * @return the modality state for no modal dialogs.
   */
  @Nonnull
  ModalityState getNoneModalityState();

  /**
   * Returns modality state which is active anytime
   *
   * @return modality state
   */
  @Nonnull
  ModalityState getAnyModalityState();

  /**
   * Returns the time of IDE start, in milliseconds since midnight, January 1, 1970 UTC.
   *
   * @return the IDE start time.
   */
  long getStartTime();

  /**
   * Returns the time in milliseconds during which IDE received no input events.
   *
   * @return the idle time of IDE.
   */
  @RequiredUIAccess
  long getIdleTime();

  /**
   * Checks if IDE is running as a command line applet or in unit test mode.
   * No UI should be shown when IDE is running in this mode.
   *
   * @return true if IDE is running in UI-less mode, false otherwise
   */
  boolean isHeadlessEnvironment();

  /**
   * Checks if IDE is running as a command line applet or in unit test mode.
   * UI can be shown (e.g. diff frame)
   *
   * @return true if IDE is running in command line  mode, false otherwise
   */
  default boolean isCommandLine() {
    return false;
  }

  @Nonnull
  default ProgressManager getProgressManager() {
    return getComponent(ProgressManager.class);
  }

  @Override
  boolean isDisposed();

  /**
   * Requests pooled thread to execute the action
   *
   * @param action to be executed
   * @return future result
   */
  @Nonnull
  Future<?> executeOnPooledThread(@Nonnull Runnable action);

  /**
   * Requests pooled thread to execute the action
   *
   * @param action to be executed
   * @return future result
   */
  @Nonnull
  <T> Future<T> executeOnPooledThread(@Nonnull Callable<T> action);

  /**
   * @return true if application is currently disposing (but not yet disposed completely)
   */
  boolean isDisposeInProgress();

  /**
   * Checks if IDE is capable of restarting itself on the current platform and with the current execution mode.
   *
   * @return true if IDE can restart itself, false otherwise.
   * @since 8.1
   */
  default boolean isRestartCapable() {
    return false;
  }

  /**
   * Exits and restarts IDE. If the current platform is not restart capable, only exits.
   */
  default void restart() {
    restart(false);
  }

  /**
   * @param exitConfirmed if true, suppresses any shutdown confirmation. However, if there are any background processes or tasks running,
   *                      a corresponding confirmation will be shown with the possibility to cancel the operation
   */
  default void restart(boolean exitConfirmed) {
    if(isRestartCapable()) {
      throw new UnsupportedOperationException("#isRestartCapable() return true, but there no implementation of #restart(boolean)");
    }
  }

  /**
   * Checks if the application is active
   *
   * @return true if one of application windows is focused, false -- otherwise
   * @since 9.0
   */
  boolean isActive();

  /**
   * @return Application icon. In sandbox icon maybe different. Size 16x16
   */
  @Nonnull
  Image getIcon();

  /**
   * @return Application icon. In sandbox icon maybe different. Better for downscale
   */
  @Nonnull
  default Image getBigIcon() {
    return getIcon();
  }

  @Nonnull
  default LocalizeValue getName() {
    return LocalizeValue.localizeTODO("Consulo");
  }

  /**
   * @return last UIAccess for application
   */
  @Nonnull
  UIAccess getLastUIAccess();

  /**
   * Return true of application is swing based, and AWT thread used as ui thread
   */
  default boolean isSwingApplication() {
    return false;
  }

  /**
   * Return true if application is unified, and used unified ui code (swing based app will return false)
   */
  default boolean isUnifiedApplication() {
    return false;
  }

  // region Deprecated stuff

  /**
   * Returns lock used for read operations, should be closed in finally block
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use runReadAction(Runnable)")
  AccessToken acquireReadActionLock();

  /**
   * Returns lock used for write operations, should be closed in finally block
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use runWriteAction(Runnable)")
  @RequiredUIAccess
  AccessToken acquireWriteActionLock(@Nonnull Class marker);

  @Deprecated
  @DeprecationInfo("Use consulo.util.SandboxUtil#isInsideSandbox")
  default boolean isInternal() {
    return false;
  }

  @Deprecated
  @DeprecationInfo("Use consulo.util.SandboxUtil#isInsideSandbox")
  default boolean isEAP() {
    return false;
  }

  /**
   * Checks if IDE is currently running unit tests. No UI should be shown when unit
   * tests are being executed.
   *
   * @return true if IDE is running unit tests, false otherwise
   */
  @Deprecated
  @DeprecationInfo("Old IDEA UnitTesting mode was dropped. This method became useless. If you want check if you inside test mode - use #isTestingMode()")
  default boolean isUnitTestMode() {
    return false;
  }
  // endregion
}
