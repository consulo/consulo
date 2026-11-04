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
package consulo.application.internal;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Lets another thread stop a read action that is still waiting for the read lock.
 * The lock wakes the waiting thread through the wake-up action it registers in {@link #beginWait(Consumer)}.
 *
 * @author VISTALL
 * @since 2026-11-04
 */
public final class ReadWaitCancellation {
    private @Nullable Thread myWaiter;
    private @Nullable Consumer<Thread> myWakeUp;
    private boolean myCancelled;
    private boolean myDone;

    public synchronized void cancel() {
        if (myCancelled || myDone) {
            return;
        }
        myCancelled = true;
        if (myWaiter != null && myWakeUp != null) {
            myWakeUp.accept(myWaiter);
        }
    }

    public synchronized boolean isCancelled() {
        return myCancelled;
    }

    /**
     * @return false when the read was cancelled before the wait started
     */
    public synchronized boolean beginWait(Consumer<Thread> wakeUp) {
        if (myCancelled) {
            return false;
        }
        myWaiter = Thread.currentThread();
        myWakeUp = wakeUp;
        return true;
    }

    /**
     * Ends the wait. After it returns, {@link #cancel()} never wakes the thread again.
     *
     * @return false when the read was cancelled during the wait
     */
    public synchronized boolean endWait() {
        myWaiter = null;
        myWakeUp = null;
        myDone = true;
        return !myCancelled;
    }
}
