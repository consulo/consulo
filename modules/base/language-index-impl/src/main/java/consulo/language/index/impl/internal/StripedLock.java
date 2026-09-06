// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.util.collection.SmartList;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

final class StripedLock {
    private static final int LOCK_SIZE = 16;
    private final ReadWriteLock[] myLocks = new ReadWriteLock[LOCK_SIZE];

    StripedLock() {
        for (int i = 0; i < myLocks.length; ++i) {
            myLocks[i] = new ReentrantReadWriteLock();
        }
    }

    ReadWriteLock getLock(int fileId) {
        int id = fileId < 0 ? -fileId : fileId;
        return myLocks[(id & 0xFF) % myLocks.length];
    }

    <T> T withReadLock(int fileId, Supplier<T> computable) {
        return withLock(fileId, computable, true);
    }

    <T> T withWriteLock(int fileId, Supplier<T> computable) {
        return withLock(fileId, computable, false);
    }

    private <T> T withLock(int fileId, Supplier<T> computable, boolean readLock) {
        ReadWriteLock readWriteLock = getLock(fileId);
        Lock lock = readLock ? readWriteLock.readLock() : readWriteLock.writeLock();
        lock.lock();
        try {
            return computable.get();
        }
        finally {
            lock.unlock();
        }
    }

    <T> T withAllLocksWriteLocked(Supplier<T> computable) {
        Lock[] locks = new Lock[myLocks.length];
        List<Exception> exceptions = new SmartList<>();
        boolean isComputed = false;
        T result = null;
        try {
            for (int i = 0; i < myLocks.length; i++) {
                locks[i] = myLocks[i].writeLock();
                locks[i].lock();
            }
            result = computable.get();
            isComputed = true;
        }
        catch (Exception e) {
            exceptions.add(e);
        }
        finally {
            for (Lock lock : locks) {
                if (lock == null) {
                    break;
                }
                else {
                    try {
                        lock.unlock();
                    }
                    catch (Exception e) {
                        exceptions.add(e);
                    }
                }
            }
        }
        if (!exceptions.isEmpty()) {
            IllegalStateException exception = new IllegalStateException("Exceptions during unlocking");
            for (Exception subException : exceptions) {
                exception.addSuppressed(subException);
            }
            throw exception;
        }
        else {
            assert isComputed : "Computation in StripedLock.withAllLocksWriteLocked was incorrect";
            return result;
        }
    }
}
