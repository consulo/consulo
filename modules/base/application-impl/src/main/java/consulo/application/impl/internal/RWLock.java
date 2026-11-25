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
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2025-03-10
 */
public interface RWLock {
    interface ReadToken {
        boolean readRequested();
    }

    static RWLock create() {
        return new StampedRWLock();
    }

    RWLock.ReadToken startRead();

    RWLock.ReadToken startTryRead();

    void endRead(RWLock.ReadToken read);

    boolean isReadLockedByThisThread();

    boolean isWriteThread();

    @Nullable Thread getWriteThread();

    boolean isWriteLocked();

    /**
     * @return true from {@link #requestWrite()} (or {@link #writeLock()}) until {@link #writeUnlock()} or
     * {@link #cancelWriteRequest()}: no new read lock is granted meanwhile
     */
    boolean isWriteRequested();

    /**
     * @return a stage completed when the write requested or held now is finished, null when no write is requested. Its
     * actions run on the thread which finishes the write
     */
    @Nullable CompletionStage<?> whenWriteFinished();

    void executeByImpatientReader(Runnable runnable);

    boolean isInImpatientReader();

    AccessToken writeSuspend();

    /**
     * Holds back new readers for the write-intent holder before it takes the write lock, so that readers cancelled before a
     * write action are not replaced by new ones meanwhile.
     */
    void requestWrite();

    /**
     * Withdraws {@link #requestWrite()} when the write lock is not taken after all.
     */
    void cancelWriteRequest();

    void writeLock();

    void writeUnlock();

    void writeIntentLock();

    void writeIntentUnlock();

    void transferWriteAction(boolean runOnEdt, Runnable action, Consumer<Runnable> scheduleTarget);

    /**
     * Runs a write action transferred to the calling thread, if one waits for it.
     */
    void pollTransferredWriteAction();

    /**
     * Takes write-intent for input on the calling UI thread, without waiting, and holds background writers back until
     * {@code leaseNanos} from now.
     *
     * @return false when another thread holds write-intent
     */
    boolean tryStartInput(long leaseNanos);

    /**
     * Holds background writers back until {@code leaseNanos} from now.
     */
    void renewInputLease(long leaseNanos);

    /**
     * @return a future completed when no thread holds write-intent
     */
    CompletableFuture<?> whenWriteIntentFree();

    /**
     * Queues input behind the input queued before: background writers are held back until {@code input} completes, at
     * most for a while.
     *
     * @return the input queued before
     */
    CompletableFuture<?> queueInput(CompletableFuture<?> input);

    /**
     * @return true while input given to {@link #queueInput} has not completed
     */
    boolean isInputQueued();

    /**
     * Background writers are not held back for input until {@link #resumeInputPriority()} - a UI thread waiting for
     * a task must not hold back the write that task may need.
     */
    void suspendInputPriority();

    void resumeInputPriority();

    /**
     * Takes write-intent for the calling thread while it still holds a read lock, without waiting - no write action can
     * run between that read and a write action the thread does next. The read lock is released as usual.
     *
     * @return false when the calling thread holds no read lock or write-intent already, when another thread holds
     * write-intent, or input on a UI thread has priority
     */
    boolean tryUpgradeToWriteIntent();

    /**
     * Write-intent taken by {@code action} does not wait while input on a UI thread has priority.
     */
    <T extends @Nullable Object> T computeAheadOfInput(Supplier<T> action);
}
