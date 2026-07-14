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
package consulo.application.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.event.ApplicationListener;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.ApplicationUtil;
import consulo.component.ComponentManager;
import java.util.concurrent.atomic.AtomicBoolean;
import consulo.localize.LocalizeValue;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author max
 */
public interface ApplicationEx extends Application {
    String LOCATOR_FILE_NAME = ".home";

    /**
     * Loads the application configuration from the specified path
     *
     * @param optionsPath Path to /config folder
     * @throws IOException
     */
    void load(@Nullable String optionsPath) throws IOException;

    boolean isLoaded();

    /**
     * @return true if this thread is inside read action.
     * @see #runReadAction(Runnable)
     */
    boolean holdsReadLock();

    /**
     * @return true if the EDT is performing write action right now.
     * @see #runWriteAction(Runnable)
     */
    boolean isWriteActionInProgress();

    /**
     * @return true if the EDT started to acquire write action but has not acquired it yet.
     * @see #runWriteAction(Runnable)
     */
    boolean isWriteActionPending();

    /**
     * Runs the action immediately when no write action is pending or in progress, otherwise defers
     * it until the write action queue is processed. The lock signals the continuation - the UI
     * thread queue is never used for the waiting.
     * <p>
     * The action may run on the calling thread (immediate case) or on the thread which finishes the
     * write action, so it must be cheap and thread-agnostic (e.g. resubmitting a task to an executor).
     */
    default void runWhenWriteActionIsCompleted(Runnable action) {
        if (!isWriteActionPending() && !isWriteActionInProgress()) {
            action.run();
            return;
        }

        AtomicBoolean executed = new AtomicBoolean();
        ApplicationListener listener = new ApplicationListener() {
            @Override
            public void afterWriteActionFinished(Object writeAction) {
                if (!isWriteActionPending() && !isWriteActionInProgress() && executed.compareAndSet(false, true)) {
                    removeApplicationListener(this);
                    action.run();
                }
            }
        };
        addApplicationListener(listener);
        // the write action might have finished between the check above and the listener registration
        if (!isWriteActionPending() && !isWriteActionInProgress() && executed.compareAndSet(false, true)) {
            removeApplicationListener(listener);
            action.run();
        }
    }

    default AccessToken startSaveBlock() {
        doNotSave();

        return new AccessToken() {
            @Override
            public void finish() {
                doNotSave(false);
            }
        };
    }

    default void doNotSave() {
        doNotSave(true);
    }

    void doNotSave(boolean value);

    boolean isDoNotSave();

    /**
     * Saves the application settings as a coroutine so it can be composed as a subroutine into a
     * larger coroutine chain (e.g. the exit sequence), instead of the blocking {@link #saveSettings()}.
     *
     * @return the settings-save coroutine, or an empty coroutine if saving is disabled
     */
    Coroutine<Object, Object> saveSettingsAsync();

    /**
     * @param force         if true, no additional confirmations will be shown. The application is guaranteed to exit
     * @param exitConfirmed if true, suppresses any shutdown confirmation. However, if there are any background processes or tasks running,
     *                      a corresponding confirmation will be shown with the possibility to cancel the operation
     */
    void exit(boolean force, boolean exitConfirmed);

    /**
     * @param exitConfirmed if true, suppresses any shutdown confirmation. However, if there are any background processes or tasks running,
     *                      a corresponding confirmation will be shown with the possibility to cancel the operation
     */
    @Override
    void restart(boolean exitConfirmed);

    /**
     * Runs modal process. For internal use only, see {@link Task}
     */
    default boolean runProcessWithProgressSynchronously(
        Runnable process,
        String progressTitle,
        boolean canBeCanceled,
        @Nullable ComponentManager project,
        JComponent parentComponent
    ) {
        return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, true, project, null, LocalizeValue.empty());
    }

    default void executeSuspendingWriteAction(@Nullable ComponentManager project, String title, Runnable runnable) {
        runnable.run();
    }

    /**
     * Runs modal process. For internal use only, see {@link Task}
     */
    default boolean runProcessWithProgressSynchronously(
        Runnable process,
        String progressTitle,
        boolean canBeCanceled,
        ComponentManager project
    ) {
        return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, true, project, null, LocalizeValue.empty());
    }

    /**
     * Runs modal or non-modal process.
     * For internal use only, see {@link Task}
     */
    boolean runProcessWithProgressSynchronously(
        Runnable process,
        String progressTitle,
        boolean canBeCanceled,
        boolean shouldShowModalWindow,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        LocalizeValue cancelText
    );

    @RequiredUIAccess
    @Deprecated
    default void assertIsDispatchThread(@Nullable JComponent component) {
        assertIsDispatchThread();
    }

    void assertTimeConsuming();

    /**
     * Grab the lock and run the action, in a non-blocking fashion
     *
     * @return true if action was run while holding the lock, false if was unable to get the lock and action was not run
     */
    @Override
    boolean tryRunReadAction(Runnable action);

    boolean isInImpatientReader();

    default void executeByImpatientReader(@RequiredReadAction Runnable runnable) throws ApplicationUtil.CannotRunReadActionException {
        throw new UnsupportedOperationException();
    }

    default boolean runWriteActionWithCancellableProgressInDispatchThread(
        LocalizeValue title,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        Consumer<? super ProgressIndicator> action
    ) {
        throw new UnsupportedOperationException();
    }

    default boolean runWriteActionWithNonCancellableProgressInDispatchThread(
        LocalizeValue title,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        Consumer<? super ProgressIndicator> action
    ) {
        throw new UnsupportedOperationException();
    }
}
