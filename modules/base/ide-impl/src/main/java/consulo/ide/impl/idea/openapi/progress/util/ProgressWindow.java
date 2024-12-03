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
package consulo.ide.impl.idea.openapi.progress.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityStateEx;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.impl.internal.progress.AbstractProgressIndicatorBase;
import consulo.application.impl.internal.progress.BlockingProgressIndicator;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.internal.ApplicationWithIntentWriteLock;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.TaskInfo;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.progress.util.ProgressDialog;
import consulo.ide.impl.progress.util.ProgressDialogFactory;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.DeprecatedMethodException;
import consulo.util.lang.EmptyRunnable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ProgressWindow extends ProgressIndicatorBase implements BlockingProgressIndicator, Disposable {
    private static final Logger LOG = Logger.getInstance(ProgressWindow.class);

    /**
     * This constant defines default delay for showing progress dialog (in millis).
     *
     * @see #setDelayInMillis(int)
     */
    public static final int DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS = 300;

    private ProgressDialog myDialog;

    public final Project myProject;
    public final boolean myShouldShowCancel;
    public LocalizeValue myCancelText = LocalizeValue.of();

    private String myTitle;

    private boolean myStoppedAlready;
    private boolean myStarted;
    protected boolean myBackgrounded;
    protected int myDelayInMillis = DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS;
    private boolean myModalityEntered;

    public ProgressWindow(boolean shouldShowCancel, Project project) {
        this(shouldShowCancel, false, project);
    }

    public ProgressWindow(boolean shouldShowCancel, boolean shouldShowBackground, @Nullable Project project) {
        this(shouldShowCancel, shouldShowBackground, project, LocalizeValue.of());
    }

    public ProgressWindow(boolean shouldShowCancel, boolean shouldShowBackground, @Nullable Project project, @Nonnull LocalizeValue cancelText) {
        this(shouldShowCancel, shouldShowBackground, project, null, cancelText);
    }

    public ProgressWindow(boolean shouldShowCancel,
                          boolean shouldShowBackground,
                          @Nullable Project project,
                          JComponent parentComponent,
                          @Nonnull LocalizeValue cancelText) {
        myProject = project;
        myShouldShowCancel = shouldShowCancel;
        myCancelText = cancelText;
        setModalityProgress(shouldShowBackground ? null : this);

        myDialog = ProgressDialogFactory.getInstance().create(this, shouldShowBackground, parentComponent, project, cancelText);

        Disposer.register(this, myDialog);

        addStateDelegate(new MyDelegate());
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ProgressWindowListener.class).progressWindowCreated(this);

        if (myProject != null) {
            Disposer.register(myProject, this);
        }
    }

    @Override
    public synchronized void start() {
        LOG.assertTrue(!isRunning());
        LOG.assertTrue(!myStoppedAlready);

        super.start();
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            prepareShowDialog();
        }

        myStarted = true;
    }

    /**
     * There is a possible case that many short (in terms of time) progress tasks are executed in a small amount of time.
     * Problem: UI blinks and looks ugly if we show progress dialog for every such task (every dialog disappears shortly).
     * Solution is to postpone showing progress dialog in assumption that the task may be already finished when it's
     * time to show the dialog.
     * <p/>
     * Default value is {@link #DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS}
     *
     * @param delayInMillis new delay time in milliseconds
     */
    public void setDelayInMillis(int delayInMillis) {
        myDelayInMillis = delayInMillis;
    }

    public synchronized boolean isStarted() {
        return myStarted;
    }

    protected void prepareShowDialog() {
        // We know at least about one use-case that requires special treatment here: many short (in terms of time) progress tasks are
        // executed in a small amount of time. Problem: UI blinks and looks ugly if we show progress dialog that disappears shortly
        // for each of them. Solution is to postpone the tasks of showing progress dialog. Hence, it will not be shown at all
        // if the task is already finished when the time comes.
        Application application = Application.get();
        application.getLastUIAccess().getScheduler().schedule(() -> {
            if (isRunning()) {
                if (myDialog != null) {
                    myDialog.copyPopupStateToWindow();
                }
                showDialog();
            }
            else {
                Disposer.dispose(this);
                final IdeFocusManager focusManager = ProjectIdeFocusManager.getInstance(myProject);
                focusManager.doWhenFocusSettlesDown(() -> focusManager.requestDefaultFocus(true), application.getDefaultModalityState());
            }
        }, getModalityState(), myDelayInMillis, TimeUnit.MILLISECONDS);
    }

    public final void enterModality() {
        if (isModalEntity() && !myModalityEntered) {
            LaterInvocator.enterModal(this, (IdeaModalityStateEx) getModalityState());
            myModalityEntered = true;
        }
    }

    public final void exitModality() {
        if (isModalEntity() && myModalityEntered) {
            myModalityEntered = false;
            LaterInvocator.leaveModal(this);
        }
    }

    /**
     * @deprecated Do not use, it's too low level and dangerous. Instead, consider using run* methods in {@link ProgressManager}
     */
    //@ApiStatus.ScheduledForRemoval(inVersion = "2022.1")
    @Deprecated
    public void startBlocking(@Nonnull Runnable init) {
        DeprecatedMethodException.report("Use ProgressManager.run*() instead");
        CompletableFuture<Object> future = new CompletableFuture<>();
        Disposer.register(this, () -> future.complete(null));
        startBlocking(init, future);
    }


    @Override
    public void startBlocking(@Nonnull Runnable init, @Nonnull CompletableFuture<?> stopCondition) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        synchronized (this) {
            LOG.assertTrue(!isRunning());
            LOG.assertTrue(!myStoppedAlready);
        }

        enterModality();
        init.run();

        try {
            try {
                ((ApplicationWithIntentWriteLock) Application.get()).runUnlockingIntendedWrite(() -> {
                    // guarantee AWT event after the future is done will be pumped and loop exited
                    stopCondition.thenRun(() -> SwingUtilities.invokeLater(EmptyRunnable.INSTANCE));
                    myDialog.startBlocking(stopCondition, this::isCancellationEvent);
                    return null;
                });
            }
            finally {
                exitModality();
                // make sure focus returns to original component (at least requested to do so)
                // before other code executed after showing modal progress
                myDialog.hideImmediately();
            }
        }
        finally {
            exitModality();
        }
    }

    public final boolean isCancellationEvent(@Nullable AWTEvent event) {
        return myShouldShowCancel &&
            event instanceof KeyEvent keyEvent &&
            event.getID() == KeyEvent.KEY_PRESSED &&
            keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE &&
            keyEvent.getModifiers() == 0;
    }

    protected void showDialog() {
        if (!isRunning() || isCanceled()) {
            return;
        }

        myDialog.show();
        if (myDialog != null) {
            myDialog.runRepaintRunnable();
        }
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        super.setIndeterminate(indeterminate);
        update();
    }

    @Override
    public synchronized void stop() {
        LOG.assertTrue(!myStoppedAlready);

        super.stop();

        UIUtil.invokeLaterIfNeeded(() -> {
            if (myDialog != null) {
                myDialog.hide();
            }

            synchronized (this) {
                myStoppedAlready = true;
            }

            Disposer.dispose(this);
        });

        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(EmptyRunnable.INSTANCE); // Just to give blocking dispatching a chance to go out.
    }

    @Nullable
    protected ProgressDialog getDialog() {
        return myDialog;
    }

    public void background() {
        if (myDialog != null) {
            myBackgrounded = true;
            myDialog.background();

            myDialog = null;
        }
    }

    protected boolean isBackgrounded() {
        return myBackgrounded;
    }

    @Override
    public void setTextValue(LocalizeValue text) {
        if (!Comparing.equal(text, getTextValue())) {
            super.setTextValue(text);
            update();
        }
    }

    @Override
    public void setFraction(double fraction) {
        if (fraction != getFraction()) {
            super.setFraction(fraction);
            update();
        }
    }

    @Override
    public void setText2Value(LocalizeValue text) {
        if (!Comparing.equal(text, getText2Value())) {
            super.setText2Value(text);
            update();
        }
    }

    private void update() {
        if (myDialog != null) {
            myDialog.update();
        }
    }

    public void setTitle(String title) {
        if (!Comparing.equal(title, myTitle)) {
            myTitle = title;
            update();
        }
    }

    public String getTitle() {
        return myTitle;
    }

    public void setCancelButtonText(LocalizeValue text) {
        if (myDialog != null) {
            myDialog.changeCancelButtonText(text);
        }
        else {
            myCancelText = text;
        }
    }

    public IdeFocusManager getFocusManager() {
        return ProjectIdeFocusManager.getInstance(myProject);
    }

    @Override
    public void dispose() {
        stopSystemActivity();
        if (isRunning()) {
            cancel();
        }
    }

    @Override
    public boolean isPopupWasShown() {
        return myDialog != null && myDialog.isPopupWasShown();
    }

    private void enableCancelButton(boolean enable) {
        if (myDialog != null) {
            myDialog.enableCancelButtonIfNeeded(enable);
        }
    }

    @Override
    public String toString() {
        return getTitle() + " " + System.identityHashCode(this) + ": running=" + isRunning() + "; canceled=" + isCanceled();
    }

    private class MyDelegate extends AbstractProgressIndicatorBase implements ProgressIndicatorEx {
        @Override
        public void cancel() {
            super.cancel();
            if (myDialog != null) {
                myDialog.cancel();
            }
        }

        @Override
        public void checkCanceled() {
            super.checkCanceled();
            // assume checkCanceled() would be called from the correct thread
            enableCancelButton(!ProgressManager.getInstance().isInNonCancelableSection());
        }

        @Override
        public void addStateDelegate(@Nonnull ProgressIndicatorEx delegate) {
            throw new IncorrectOperationException();
        }

        @Override
        public void finish(@Nonnull TaskInfo task) {
        }

        @Override
        public boolean isFinished(@Nonnull TaskInfo task) {
            return true;
        }

        @Override
        public boolean wasStarted() {
            return false;
        }

        @Override
        public void processFinish() {
        }
    }
}
