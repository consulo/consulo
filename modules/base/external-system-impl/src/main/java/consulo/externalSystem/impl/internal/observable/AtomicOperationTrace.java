/*
 * Copyright 2013-2026 consulo.io
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
package consulo.externalSystem.impl.internal.observable;

import consulo.disposer.Disposable;
import consulo.disposer.util.DisposableList;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public final class AtomicOperationTrace {
    private final String myName;

    private final AtomicInteger myInProgressCount = new AtomicInteger();

    private final DisposableList<Runnable> myStartListeners = DisposableList.create();

    private final DisposableList<Runnable> myFinishListeners = DisposableList.create();

    public AtomicOperationTrace(String name) {
        myName = name;
    }

    public String getName() {
        return myName;
    }

    public boolean isOperationInProgress() {
        return myInProgressCount.get() > 0;
    }

    public void traceStart() {
        if (myInProgressCount.getAndIncrement() == 0) {
            fire(myStartListeners);
        }
    }

    public void traceFinish() {
        if (myInProgressCount.getAndUpdate(count -> Math.max(count - 1, 0)) == 1) {
            fire(myFinishListeners);
        }
    }

    public void whenOperationStarted(Runnable listener) {
        whenOperationStarted(null, listener);
    }

    public void whenOperationFinished(Runnable listener) {
        whenOperationFinished(null, listener);
    }

    public void whenOperationStarted(@Nullable Disposable parentDisposable, Runnable listener) {
        addListener(myStartListeners, parentDisposable, listener);
    }

    public void whenOperationFinished(@Nullable Disposable parentDisposable, Runnable listener) {
        addListener(myFinishListeners, parentDisposable, listener);
    }

    private static void addListener(DisposableList<Runnable> listeners, @Nullable Disposable parentDisposable, Runnable listener) {
        if (parentDisposable == null) {
            listeners.add(listener);
        }
        else {
            listeners.add(listener, parentDisposable);
        }
    }

    private static void fire(DisposableList<Runnable> listeners) {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    @Override
    public String toString() {
        return myName + ": " + myInProgressCount.get();
    }
}
