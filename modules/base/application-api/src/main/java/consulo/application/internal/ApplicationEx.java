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

import java.util.concurrent.Executor;
import consulo.application.progress.Task;
import consulo.application.util.ApplicationUtil;
import consulo.component.ComponentManager;
import consulo.localize.LocalizeValue;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * @return true if a write action is running right now, on any thread.
     * @see #runWriteAction(Runnable)
     */
    boolean isWriteActionInProgress();

    /**
     * @return true when the calling thread is the one owning write actions
     */
    boolean isWriteThread();

    default boolean isCurrentWriteOnUIThread() {
        return false;
    }

    /**
     * The executor write actions run on - a single platform thread.
     * <p>
     * The write lock identifies its owner by {@code Thread.currentThread()}, and each step of a coroutine may run on
     * a different virtual thread of the application coroutine context, so the ownership could not span the steps of
     * one coroutine. A coroutine step taking the write lock moves here first.
     */
    Executor getWriteExecutor();

    /**
     * Runs {@code read} in a read action, then {@code write} with its result in a write action - and no other write action
     * runs in between.
     * <p>
     * When the lock can, the read switches straight to write-intent. Otherwise write-intent is taken after the read, and if a
     * write action ran in between, {@code read} runs once more under write-intent. So {@code read} may run twice and must
     * not have side effects beyond its result.
     */
    <R, O> O runReadAndWriteAction(Class<?> actionClass, Supplier<R> read, Function<R, O> write);

    /**
     * Grows with every write action. While a thread holds the write-intent lock it changes only by write actions of that
     * thread, so an unchanged value there means no write action ran since it was read.
     */
    long getWriteEpoch();

    /**
     * A write action started inside {@code action} does not wait for a pause in the input of a UI thread - for a short write
     * the input itself waits for, such as a document commit.
     */
    <T extends @Nullable Object> T runAheadOfInput(Supplier<T> action);

    /**
     * @return true if a thread started to acquire write action but has not acquired it yet.
     * @see #runWriteAction(Runnable)
     */
    boolean isWriteActionPending();

    /**
     * Runs the action immediately when no write action is pending or in progress, otherwise defers
     * it until that write action is finished or suspended. The lock signals the continuation - the UI
     * thread queue is never used for the waiting.
     * <p>
     * The action may run on the calling thread (immediate case) or on the thread which finishes the
     * write action, so it must be cheap and thread-agnostic (e.g. resubmitting a task to an executor).
     */
    void runWhenWriteActionIsCompleted(Runnable action);

    /**
     * Runs input - a typed character, an editor action - on the UI thread of {@code uiAccess}. It runs right away holding
     * write-intent, so a write action it does never waits for a background one, unless another thread holds write-intent
     * now: then it runs as soon as that thread releases it, and the UI thread doesn't wait for that meanwhile. Background
     * writers are held back while input keeps coming, until it pauses. Must be called on that UI thread.
     *
     * @return a future completed when the input has run
     */
    CompletableFuture<?> runInput(UIAccess uiAccess, Runnable input);

    /**
     * Same as {@link #runInput(UIAccess, Runnable)} for input which completes asynchronously: write-intent is held only while
     * {@code input} is called.
     */
    <T> CompletableFuture<T> runInputAsync(UIAccess uiAccess, Supplier<CompletableFuture<T>> input);

    /**
     * True while the calling thread runs input given to {@link #runInput} - code which is part of that input runs right away
     * rather than through {@link #runInput}, which would queue it behind the input itself. Input given while one runs - from
     * an event loop nested in it - waits for it.
     */
    boolean isInsideInput();

    /**
     * True while input given to {@link #runInput} has not run yet - it waits for a background write action to let go of
     * write-intent. A pointer event which moves the caret goes through {@link #runInput} meanwhile, behind that input: moving the
     * caret first would put the waiting input where the pointer went.
     */
    boolean isInputPending();

    /**
     * @return the number of actions waiting in {@link #runWhenWriteActionIsCompleted(Runnable)}
     */
    int writeActionFollowupsSize();

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
}
