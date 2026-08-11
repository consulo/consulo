/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.Clipboard;
import consulo.ui.event.ModalityStateListener;
import consulo.ui.internal.UIInternal;
import consulo.util.concurrent.AsyncResult;
import consulo.util.concurrent.internal.ThreadAssertion;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;

import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2016-06-11
 */
public interface UIAccess extends Executor, UserDataHolder {
    Key<UIAccess> KEY = Key.of(UIAccess.class);

    /**
     * @return if current thread has access to UI write mode.
     */
    static boolean isUIThread() {
        return UIInternal.get()._UIAccess_isUIThread();
    }

    @Deprecated
    @RequiredUIAccess
    static UIAccess get() {
        return current();
    }

    /**
     * If we're inside UI thread, we can get UI access.
     *
     * @return ui access - or throw exception.
     */
    @RequiredUIAccess
    static UIAccess current() {
        assertIsUIThread();

        return UIInternal.get()._UIAccess_get();
    }

    @RequiredUIAccess
    static void assertIsUIThread() {
        ThreadAssertion.assertTrue(!isUIThread(), "Call must be called inside UI thread");
    }

    static void assetIsNotUIThread() {
        ThreadAssertion.assertTrue(isUIThread(), "Call must be called outside UI thread");
    }

    static void addModalityStateListener(ModalityStateListener listener, Disposable parentDisposable) {
        UIInternal.get().addModalityStateListener(listener, parentDisposable);
    }

    /**
     * Every ui the application is showing at the moment. A frontend which draws into a single window answers with
     * the one access it has, the browser frontend with one per tab.
     */
    static Collection<UIAccess> listAll() {
        return UIInternal.get()._UIAccess_all();
    }

    /**
     * Whether more than one ui can be attached to the running application at the same time, as the browser
     * frontend allows with its tabs. A frontend which says so must accept a project being moved between them.
     */
    static boolean supportsMultipleUI() {
        return UIInternal.get()._UIAccess_supportsMultipleUI();
    }

    Clipboard getClipboard();

    @RequiredUIAccess
    default int getEventCount() {
        assertIsUIThread();
        return -1;
    }

    /**
     * Calling Runnable#run() will restore event count from initial method call.
     */
    @RequiredUIAccess
    default Runnable markEventCount() {
        assertIsUIThread();
        return () -> {
        };
    }

    default boolean isValid() {
        return true;
    }

    void give(@RequiredUIAccess Runnable runnable);

    default CompletableFuture<?> giveAsync(@RequiredUIAccess Runnable runnable) {
        return giveAsync(() -> {
            runnable.run();
            return null;
        });
    }

    <T> CompletableFuture<T> giveAsync(@RequiredUIAccess Supplier<T> supplier);

    default void giveAndWait(@RequiredUIAccess Runnable runnable) {
        try {
            giveAsync(runnable).get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    default <T> T giveAndWaitIfNeed(@RequiredUIAccess Supplier<T> supplier) {
        Object[] value = new Object[1];
        giveAndWaitIfNeed((Runnable) () -> value[0] = supplier.get());
        return (T) value[0];
    }

    default void giveIfNeed(@RequiredUIAccess Runnable runnable) {
        if (isUIThread()) {
            runnable.run();
        }
        else {
            give(runnable);
        }
    }

    default void giveAndWaitIfNeed(@RequiredUIAccess Runnable runnable) {
        if (isUIThread()) {
            runnable.run();
        }
        else {
            giveAndWait(runnable);
        }
    }

    default boolean isHeadless() {
        return false;
    }

    default boolean isInModalContext() {
        return false;
    }

    @Override
    default void execute(@RequiredUIAccess Runnable command) {
        give(command);
    }

    UIAccessScheduler getScheduler();
}