/*
 * Copyright 2013-2025 consulo.io
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

import consulo.application.AccessToken;
import consulo.util.collection.Lists;

import java.util.List;
import java.util.concurrent.locks.StampedLock;

/**
 * @author VISTALL
 * @since 2025-03-10
 */
public class UnifiedRWLock implements RWLock {
    public record UnifiedReadToken(long token) implements ReadToken {

        @Override
        public boolean readRequested() {
            return false;
        }
    }

    private final StampedLock myLock = new StampedLock();

    private volatile Thread myWriteThread;

    private final List<Thread> myReadThreads = Lists.newLockFreeCopyOnWriteList();

    @Override
    public UnifiedReadToken startRead() {
        if (isReadLockedByThisThread()) {
            return new UnifiedReadToken(-1);
        }

        long l = myLock.readLock();
        myReadThreads.add(Thread.currentThread());
        return new UnifiedReadToken(l);
    }

    @Override
    public ReadToken startTryRead() {
        if (myLock.isWriteLocked()) {
            return null;
        }

        return startRead();
    }

    @Override
    public void endRead(ReadToken read) {
        long token = ((UnifiedReadToken) read).token();
        if (token == -1) {
            return;
        }

        myLock.unlockRead(token);

        myReadThreads.remove(Thread.currentThread());
    }

    @Override
    public boolean isReadLockedByThisThread() {
        return myReadThreads.contains(Thread.currentThread());
    }

    @Override
    public boolean isWriteThread() {
        return myWriteThread == Thread.currentThread();
    }

    @Override
    public boolean isWriteLocked() {
        return myLock.isWriteLocked();
    }

    @Override
    public void executeByImpatientReader(Runnable runnable) {
        runnable.run();
    }

    @Override
    public boolean isInImpatientReader() {
        return false;
    }

    @Override
    public AccessToken writeSuspend() {
        return null;
    }

    @Override
    public void writeLock() {
        myLock.writeLock();
        myWriteThread = Thread.currentThread();
    }

    @Override
    public void writeUnlock() {
        if (myLock.tryUnlockWrite()) {
            myWriteThread = null;
        }
    }

    @Override
    public void writeIntentLock() {
        writeLock();
    }

    @Override
    public void writeIntentUnlock() {
        writeUnlock();
    }
}
