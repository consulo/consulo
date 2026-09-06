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
package consulo.application.impl.internal;

import consulo.ui.UIAccess;
import consulo.util.lang.ExceptionUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

final class WriteActionTransfer {
    private record Transfer(boolean runOnEdt, Runnable trampoline) {
    }

    private final AtomicReference<Transfer> myPendingTransfer = new AtomicReference<>();
    private final Consumer<Thread> myWriteThreadSetter;

    WriteActionTransfer(Consumer<Thread> writeThreadSetter) {
        myWriteThreadSetter = writeThreadSetter;
    }

    void transfer(boolean runOnEdt, Runnable action, Consumer<Runnable> scheduleTarget) {
        Thread source = Thread.currentThread();

        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Runnable trampoline = () -> {
            myWriteThreadSetter.accept(Thread.currentThread());
            try {
                action.run();
            }
            catch (Throwable t) {
                error.set(t);
            }
            finally {
                myWriteThreadSetter.accept(source);
                finished.countDown();
                LockSupport.unpark(source);
            }
        };

        myPendingTransfer.set(new Transfer(runOnEdt, trampoline));
        scheduleTarget.accept(this::poll);

        while (finished.getCount() != 0) {
            LockSupport.parkNanos(this, 1_000_000);
        }

        Throwable t = error.get();
        if (t != null) {
            ExceptionUtil.rethrow(t);
        }
    }

    void poll() {
        Transfer transfer = myPendingTransfer.get();
        if (transfer == null) {
            return;
        }
        boolean edt = UIAccess.isUIThread();
        if (transfer.runOnEdt() == edt && myPendingTransfer.compareAndSet(transfer, null)) {
            transfer.trampoline().run();
        }
    }
}
