/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.progress;

import com.intellij.CommonBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.EdtReplacementThread;
import com.intellij.openapi.project.DumbModeAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ExceptionUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.function.Consumer;

/**
 * Intended to run tasks, both modal and non-modal (backgroundable)
 * Example of use:
 * <pre>
 * new Task.Backgroundable(project, "Synchronizing data", true) {
 *  public void run(ProgressIndicator indicator) {
 *    indicator.setText("Loading changes");
 *    indicator.setFraction(0.0);
 *    // some code
 *    indicator.setFraction(1.0);
 *  }
 * }.setCancelText("Stop loading").queue();
 * </pre>
 *
 * @see ProgressManager#run(Task)
 */
public abstract class Task implements TaskInfo, Progressive {
  private static final Logger LOG = Logger.getInstance(Task.class);

  protected final Project myProject;
  @Nullable
  protected final JComponent myParentComponent;

  protected String myTitle;
  private final boolean myCanBeCancelled;

  private String myCancelText = CommonBundle.getCancelButtonText();
  private String myCancelTooltipText = CommonBundle.getCancelButtonText();

  public Task(@Nullable Project project, @Nullable JComponent parentComponent, @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title, boolean canBeCancelled) {
    myProject = project;
    myParentComponent = parentComponent;
    myTitle = title;
    myCanBeCancelled = canBeCancelled;
  }

  /**
   * This callback will be invoked on AWT dispatch thread.
   * <p>
   * Callback executed when run() throws {@link ProcessCanceledException} or if its {@link ProgressIndicator} was canceled.
   */
  @RequiredUIAccess
  public void onCancel() {
  }

  /**
   * This callback will be invoked on AWT dispatch thread.
   */
  @RequiredUIAccess
  public void onSuccess() {
  }

  /**
   * This callback will be invoked on AWT dispatch thread.
   * <p>
   * Callback executed when run() throws an exception (except PCE).
   *
   * @deprecated use {@link #onThrowable(Throwable)} instead
   */
  @Deprecated
  public void onError(@Nonnull Exception error) {
    LOG.error(error);
  }

  /**
   * This callback will be invoked on AWT dispatch thread.
   * <p>
   * Callback executed when run() throws an exception (except PCE).
   */
  public void onThrowable(@Nonnull Throwable error) {
    if (error instanceof Exception) {
      onError((Exception)error);
    }
    else {
      LOG.error(error);
    }
  }

  /**
   * This callback will be invoked on AWT dispatch thread, after other specific handlers
   */
  @RequiredUIAccess
  public void onFinished() {
  }

  public final Project getProject() {
    return myProject;
  }

  public final void queue() {
    ProgressManager.getInstance().run(this);
  }

  /**
   * Specifies the thread to run callbacks on. See {@link EdtReplacementThread} documentation for more info.
   */
  @Nonnull
  public EdtReplacementThread whereToRunCallbacks() {
    return EdtReplacementThread.EDT_WITH_IW;
  }

  @Nullable
  public final JComponent getParentComponent() {
    return myParentComponent;
  }

  @Override
  @Nonnull
  public final String getTitle() {
    return myTitle;
  }

  @Nonnull
  public final Task setTitle(@Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title) {
    myTitle = title;
    return this;
  }

  @Override
  public final String getCancelText() {
    return myCancelText;
  }

  @Nonnull
  public final Task setCancelText(String cancelText) {
    myCancelText = cancelText;
    return this;
  }

  @Nullable
  public NotificationInfo getNotificationInfo() {
    return null;
  }

  @Nullable
  public NotificationInfo notifyFinished() {
    return getNotificationInfo();
  }

  public boolean isHeadless() {
    return ApplicationManager.getApplication().isUnitTestMode() || ApplicationManager.getApplication().isHeadlessEnvironment();
  }

  @Nonnull
  public final Task setCancelTooltipText(String cancelTooltipText) {
    myCancelTooltipText = cancelTooltipText;
    return this;
  }

  @Override
  public final String getCancelTooltipText() {
    return myCancelTooltipText;
  }

  @Override
  public final boolean isCancellable() {
    return myCanBeCancelled;
  }

  public abstract boolean isModal();

  @Nonnull
  public final Modal asModal() {
    if (isModal()) {
      return (Modal)this;
    }
    throw new IllegalStateException("Not a modal task");
  }

  @Nonnull
  public final Backgroundable asBackgroundable() {
    if (!isModal()) {
      return (Backgroundable)this;
    }
    throw new IllegalStateException("Not a backgroundable task");
  }

  public abstract static class Backgroundable extends Task implements PerformInBackgroundOption {

    public static void queue(@Nullable Project project, @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title, @Nonnull Consumer<ProgressIndicator> consumer) {
      queue(project, title, true, consumer);
    }

    public static void queue(@Nullable Project project, @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title, boolean canBeCancelled, @Nonnull Consumer<ProgressIndicator> consumer) {
      queue(project, title, canBeCancelled, null, consumer);
    }

    public static void queue(@Nullable Project project,
                             @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title,
                             boolean canBeCancelled,
                             @Nullable PerformInBackgroundOption backgroundOption,
                             @Nonnull Consumer<ProgressIndicator> consumer) {
      new Backgroundable(project, title, canBeCancelled, backgroundOption) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          consumer.accept(indicator);
        }
      }.queue();
    }

    public static void queue(@Nullable Project project,
                             @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title,
                             boolean canBeCancelled,
                             @Nullable PerformInBackgroundOption backgroundOption,
                             @Nonnull Consumer<ProgressIndicator> consumer,
                             @Nonnull Runnable onSuccess) {
      new Backgroundable(project, title, canBeCancelled, backgroundOption) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          consumer.accept(indicator);
        }

        @RequiredUIAccess
        @Override
        public void onSuccess() {
          onSuccess.run();
        }
      }.queue();
    }

    protected final PerformInBackgroundOption myBackgroundOption;

    public Backgroundable(@Nullable Project project, @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title) {
      this(project, title, true);
    }

    public Backgroundable(@Nullable Project project, @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title, boolean canBeCancelled) {
      this(project, title, canBeCancelled, null);
    }

    public Backgroundable(@Nullable Project project,
                          @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title,
                          boolean canBeCancelled,
                          @Nullable PerformInBackgroundOption backgroundOption) {
      super(project, null, title, canBeCancelled);
      myBackgroundOption = backgroundOption;
      if (StringUtil.isEmptyOrSpaces(title)) {
        LOG.warn("Empty title for backgroundable task.", new Throwable());
      }
    }

    @Override
    public boolean shouldStartInBackground() {
      return myBackgroundOption == null || myBackgroundOption.shouldStartInBackground();
    }

    @Override
    public void processSentToBackground() {
      if (myBackgroundOption != null) {
        myBackgroundOption.processSentToBackground();
      }
    }

    @Override
    public final boolean isModal() {
      return false;
    }

    public boolean isConditionalModal() {
      return false;
    }

    /**
     * to remove in IDEA 16
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public DumbModeAction getDumbModeAction() {
      return DumbModeAction.NOTHING;
    }
  }

  public abstract static class Modal extends Task {
    public Modal(@Nullable Project project, @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title, boolean canBeCancelled) {
      super(project, null, title, canBeCancelled);
    }

    public Modal(@Nullable Project project, @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title, @Nullable JComponent parentComponent, boolean canBeCancelled) {
      super(project, parentComponent, title, canBeCancelled);
    }

    @Override
    public final boolean isModal() {
      return true;
    }
  }

  public abstract static class ConditionalModal extends Backgroundable {
    public ConditionalModal(@Nullable Project project,
                            @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title,
                            boolean canBeCancelled,
                            @Nonnull PerformInBackgroundOption backgroundOption) {
      super(project, title, canBeCancelled, backgroundOption);
    }

    @Override
    public final boolean isConditionalModal() {
      return true;
    }
  }

  public static class NotificationInfo {
    private final String myNotificationName;
    private final String myNotificationTitle;
    private final String myNotificationText;
    private final boolean myShowWhenFocused;

    public NotificationInfo(@Nonnull final String notificationName, @Nonnull final String notificationTitle, @Nonnull final String notificationText) {
      this(notificationName, notificationTitle, notificationText, false);
    }

    public NotificationInfo(@Nonnull final String notificationName, @Nonnull final String notificationTitle, @Nonnull final String notificationText, final boolean showWhenFocused) {
      myNotificationName = notificationName;
      myNotificationTitle = notificationTitle;
      myNotificationText = notificationText;
      myShowWhenFocused = showWhenFocused;
    }

    @Nonnull
    public String getNotificationName() {
      return myNotificationName;
    }

    @Nonnull
    public String getNotificationTitle() {
      return myNotificationTitle;
    }

    @Nonnull
    public String getNotificationText() {
      return myNotificationText;
    }

    public boolean isShowWhenFocused() {
      return myShowWhenFocused;
    }
  }

  public abstract static class WithResult<T, E extends Exception> extends Task.Modal {
    private final Ref<T> myResult = Ref.create();
    private final Ref<Throwable> myError = Ref.create();

    public WithResult(@Nullable Project project, @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String title, boolean canBeCancelled) {
      super(project, title, canBeCancelled);
    }

    @Override
    public final void run(@Nonnull ProgressIndicator indicator) {
      try {
        myResult.set(compute(indicator));
      }
      catch (Throwable t) {
        myError.set(t);
      }
    }

    protected abstract T compute(@Nonnull ProgressIndicator indicator) throws E;

    @SuppressWarnings("unchecked")
    public T getResult() throws E {
      Throwable t = myError.get();
      ExceptionUtil.rethrowUnchecked(t);
      if (t != null) throw (E)t;
      return myResult.get();
    }
  }
}