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
package consulo.application.progress;

import consulo.annotation.DeprecationInfo;
import consulo.application.localize.ApplicationLocalize;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

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

    protected final ComponentManager myProject;
    @Nullable
    protected final JComponent myParentComponent;

    protected LocalizeValue myTitle;
    private final boolean myCanBeCancelled;

    private LocalizeValue myCancelText = ApplicationLocalize.taskButtonCancel();
    private LocalizeValue myCancelTooltipText = ApplicationLocalize.taskButtonCancel();

    public Task(
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        LocalizeValue title,
        boolean canBeCancelled
    ) {
        myProject = project;
        myParentComponent = parentComponent;
        myTitle = title;
        myCanBeCancelled = canBeCancelled;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public Task(
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
         String title,
        boolean canBeCancelled
    ) {
        this(project, parentComponent, LocalizeValue.of(title), canBeCancelled);
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
    public void onError(Exception error) {
        LOG.error(error);
    }

    /**
     * This callback will be invoked on AWT dispatch thread.
     * <p>
     * Callback executed when run() throws an exception (except PCE).
     */
    public void onThrowable(Throwable throwable) {
        if (throwable instanceof Exception exception) {
            onError(exception);
        }
        else {
            LOG.error(throwable);
        }
    }

    /**
     * This callback will be invoked on AWT dispatch thread, after other specific handlers
     */
    @RequiredUIAccess
    public void onFinished() {
    }

    public final ComponentManager getProject() {
        return myProject;
    }

    public final void queue() {
        ProgressManager.getInstance().run(this);
    }

    @Nullable
    public final JComponent getParentComponent() {
        return myParentComponent;
    }

    @Override
    
    public final String getTitle() {
        return myTitle.get();
    }

    
    public final Task setTitle(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    
    public final Task setTitle( String title) {
        myTitle = LocalizeValue.of(title);
        return this;
    }

    
    @Override
    public LocalizeValue getCancelTextValue() {
        return myCancelText;
    }

    
    @Override
    public LocalizeValue getCancelTooltipTextValue() {
        return myCancelTooltipText;
    }

    
    public final Task setCancelText(LocalizeValue cancelText) {
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

    @Deprecated
    public boolean isHeadless() {
        return false;
    }

    
    public final Task setCancelTooltipText(LocalizeValue cancelTooltipText) {
        myCancelTooltipText = cancelTooltipText;
        return this;
    }

    @Override
    public final boolean isCancellable() {
        return myCanBeCancelled;
    }

    public abstract boolean isModal();

    
    public final Modal asModal() {
        if (isModal()) {
            return (Modal)this;
        }
        throw new IllegalStateException("Not a modal task");
    }

    
    public final Backgroundable asBackgroundable() {
        if (!isModal()) {
            return (Backgroundable)this;
        }
        throw new IllegalStateException("Not a backgroundable task");
    }

    public abstract static class Backgroundable extends Task implements PerformInBackgroundOption {
        public static void queue(
            @Nullable ComponentManager project,
            LocalizeValue title,
            Consumer<ProgressIndicator> consumer
        ) {
            queue(project, title, true, consumer);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public static void queue(
            @Nullable ComponentManager project,
             String title,
            Consumer<ProgressIndicator> consumer
        ) {
            queue(project, title, true, consumer);
        }

        public static void queue(
            @Nullable ComponentManager project,
            LocalizeValue title,
            boolean canBeCancelled,
            Consumer<ProgressIndicator> consumer
        ) {
            queue(project, title, canBeCancelled, null, consumer);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public static void queue(
            @Nullable ComponentManager project,
             String title,
            boolean canBeCancelled,
            Consumer<ProgressIndicator> consumer
        ) {
            queue(project, title, canBeCancelled, null, consumer);
        }

        public static void queue(
            @Nullable ComponentManager project,
            LocalizeValue title,
            boolean canBeCancelled,
            @Nullable PerformInBackgroundOption backgroundOption,
            Consumer<ProgressIndicator> consumer
        ) {
            new Backgroundable(project, title, canBeCancelled, backgroundOption) {
                @Override
                public void run(ProgressIndicator indicator) {
                    try {
                        consumer.accept(indicator);
                    }
                    catch (ProcessCanceledException e) {
                        throw e;
                    }
                    catch (Throwable e) {
                        LOG.error(e);
                    }
                }
            }.queue();
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public static void queue(
            @Nullable ComponentManager project,
             String title,
            boolean canBeCancelled,
            @Nullable PerformInBackgroundOption backgroundOption,
            Consumer<ProgressIndicator> consumer
        ) {
            queue(project, LocalizeValue.of(title), canBeCancelled, backgroundOption, consumer);
        }

        public static void queue(
            @Nullable ComponentManager project,
            LocalizeValue title,
            boolean canBeCancelled,
            @Nullable PerformInBackgroundOption backgroundOption,
            Consumer<ProgressIndicator> consumer,
            Runnable onSuccess
        ) {
            new Backgroundable(project, title, canBeCancelled, backgroundOption) {
                @Override
                public void run(ProgressIndicator indicator) {
                    try {
                        consumer.accept(indicator);
                    }
                    catch (ProcessCanceledException e) {
                        throw e;
                    }
                    catch (Throwable e) {
                        LOG.error(e);
                    }
                }

                @RequiredUIAccess
                @Override
                public void onSuccess() {
                    onSuccess.run();
                }
            }.queue();
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public static void queue(
            @Nullable ComponentManager project,
             String title,
            boolean canBeCancelled,
            @Nullable PerformInBackgroundOption backgroundOption,
            Consumer<ProgressIndicator> consumer,
            Runnable onSuccess
        ) {
            queue(project, LocalizeValue.of(title), canBeCancelled, backgroundOption, consumer, onSuccess);
        }

        protected final PerformInBackgroundOption myBackgroundOption;

        public Backgroundable(@Nullable ComponentManager project, LocalizeValue title) {
            this(project, title, true);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public Backgroundable(@Nullable ComponentManager project,  String title) {
            this(project, title, true);
        }

        public Backgroundable(
            @Nullable ComponentManager project,
            LocalizeValue title,
            boolean canBeCancelled
        ) {
            this(project, title, canBeCancelled, null);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public Backgroundable(
            @Nullable ComponentManager project,
             String title,
            boolean canBeCancelled
        ) {
            this(project, title, canBeCancelled, null);
        }

        public Backgroundable(
            @Nullable ComponentManager project,
            LocalizeValue title,
            boolean canBeCancelled,
            @Nullable PerformInBackgroundOption backgroundOption
        ) {
            super(project, null, title, canBeCancelled);
            myBackgroundOption = backgroundOption;
            if (title.isEmpty()) {
                LOG.warn("Empty title for backgroundable task.", new Throwable());
            }
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public Backgroundable(
            @Nullable ComponentManager project,
             String title,
            boolean canBeCancelled,
            @Nullable PerformInBackgroundOption backgroundOption
        ) {
            this(project, LocalizeValue.of(title), canBeCancelled, backgroundOption);
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
        public static void queue(
            @Nullable ComponentManager project,
            LocalizeValue title,
            boolean canBeCancelled,
            Consumer<ProgressIndicator> consumer
        ) {
            new Modal(project, title, canBeCancelled) {
                @Override
                public void run(ProgressIndicator indicator) {
                    consumer.accept(indicator);
                }
            }.queue();
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public static void queue(
            @Nullable ComponentManager project,
             String title,
            boolean canBeCancelled,
            Consumer<ProgressIndicator> consumer
        ) {
            queue(project, LocalizeValue.of(title), canBeCancelled, consumer);
        }

        public static void queue(
            @Nullable ComponentManager project,
            LocalizeValue title,
            boolean canBeCancelled,
            Consumer<ProgressIndicator> consumer,
            Runnable onSuccess
        ) {
            new Modal(project, title, canBeCancelled) {
                @Override
                public void run(ProgressIndicator indicator) {
                    try {
                        consumer.accept(indicator);
                    }
                    catch (ProcessCanceledException e) {
                        throw e;
                    }
                    catch (Throwable e) {
                        LOG.error(e);
                    }
                }

                @RequiredUIAccess
                @Override
                public void onSuccess() {
                    onSuccess.run();
                }
            }.queue();
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public static void queue(
            @Nullable ComponentManager project,
             String title,
            boolean canBeCancelled,
            Consumer<ProgressIndicator> consumer,
            Runnable onSuccess
        ) {
            queue(project, LocalizeValue.of(title), canBeCancelled, consumer, onSuccess);
        }

        public Modal(@Nullable ComponentManager project, LocalizeValue title, boolean canBeCancelled) {
            super(project, null, title, canBeCancelled);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public Modal(
            @Nullable ComponentManager project,
             String title,
            boolean canBeCancelled
        ) {
            super(project, null, title, canBeCancelled);
        }

        public Modal(
            @Nullable ComponentManager project,
            LocalizeValue title,
            @Nullable JComponent parentComponent,
            boolean canBeCancelled
        ) {
            super(project, parentComponent, title, canBeCancelled);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public Modal(
            @Nullable ComponentManager project,
             String title,
            @Nullable JComponent parentComponent,
            boolean canBeCancelled
        ) {
            super(project, parentComponent, title, canBeCancelled);
        }

        @Override
        public final boolean isModal() {
            return true;
        }
    }

    public abstract static class ConditionalModal extends Backgroundable {
        public ConditionalModal(
            @Nullable ComponentManager project,
            LocalizeValue title,
            boolean canBeCancelled,
            PerformInBackgroundOption backgroundOption
        ) {
            super(project, title, canBeCancelled, backgroundOption);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public ConditionalModal(
            @Nullable ComponentManager project,
             String title,
            boolean canBeCancelled,
            PerformInBackgroundOption backgroundOption
        ) {
            super(project, title, canBeCancelled, backgroundOption);
        }

        @Override
        public final boolean isConditionalModal() {
            return true;
        }
    }

    public static class NotificationInfo {
        private final LocalizeValue myNotificationName;
        private final LocalizeValue myNotificationTitle;
        private final LocalizeValue myNotificationText;
        private final boolean myShowWhenFocused;

        public NotificationInfo(
            LocalizeValue notificationName,
            LocalizeValue notificationTitle,
            LocalizeValue notificationText
        ) {
            this(notificationName, notificationTitle, notificationText, false);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public NotificationInfo(
            String notificationName,
            String notificationTitle,
            String notificationText
        ) {
            this(notificationName, notificationTitle, notificationText, false);
        }

        public NotificationInfo(
            LocalizeValue notificationName,
            LocalizeValue notificationTitle,
            LocalizeValue notificationText,
            boolean showWhenFocused
        ) {
            myNotificationName = notificationName;
            myNotificationTitle = notificationTitle;
            myNotificationText = notificationText;
            myShowWhenFocused = showWhenFocused;
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public NotificationInfo(
            String notificationName,
            String notificationTitle,
            String notificationText,
            boolean showWhenFocused
        ) {
            this(
                LocalizeValue.of(notificationName),
                LocalizeValue.of(notificationTitle),
                LocalizeValue.of(notificationText),
                showWhenFocused
            );
        }

        
        public String getNotificationName() {
            return myNotificationName.get();
        }

        
        public String getNotificationTitle() {
            return myNotificationTitle.get();
        }

        
        public String getNotificationText() {
            return myNotificationText.get();
        }

        public boolean isShowWhenFocused() {
            return myShowWhenFocused;
        }
    }
}