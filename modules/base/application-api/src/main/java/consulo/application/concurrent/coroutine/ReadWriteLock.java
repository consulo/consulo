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
package consulo.application.concurrent.coroutine;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.ApplicationWithIntentWriteLock;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.CoroutineStep;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A read action followed by a write action with its result, and no other write action in between - unlike a {@link ReadLock}
 * step followed by a {@link WriteLock} step, which releases the read lock before the write-intent lock is taken.
 * <p>
 * {@link #apply} holds a read lock for the read part: a write action waits for it. The read part may run twice (the second
 * time under the write-intent lock, when a write action ran right after the first run).
 * <p>
 * {@link #nonBlocking} never holds a write action back: the read part is cancelled when a write action comes and restarts
 * after it, and it runs again when a write action ran before the write part could start.
 * <p>
 * Either way the read part must not have side effects beyond its result. It may finish the coroutine early, then the write
 * part does not run.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public final class ReadWriteLock<I, R, O> extends CoroutineStep<I, O> {
    public static <I, R, O> ReadWriteLock<I, R, O> apply(
        @RequiredReadAction Function<I, R> read,
        @RequiredWriteAction Function<R, O> write
    ) {
        return new ReadWriteLock<>((i, c) -> read.apply(i), (r, c) -> write.apply(r));
    }

    public static <I, R, O> ReadWriteLock<I, R, O> apply(
        @RequiredReadAction BiFunction<I, Continuation<?>, R> read,
        @RequiredWriteAction BiFunction<R, Continuation<?>, O> write
    ) {
        return new ReadWriteLock<>(read, write);
    }

    public static <I, R, O> ReadWriteLock<I, R, O> nonBlocking(
        @RequiredReadAction Function<I, R> read,
        @RequiredWriteAction Function<R, O> write
    ) {
        return new ReadWriteLock<>((i, c) -> read.apply(i), (r, c) -> write.apply(r), true);
    }

    public static <I, R, O> ReadWriteLock<I, R, O> nonBlocking(
        @RequiredReadAction BiFunction<I, Continuation<?>, R> read,
        @RequiredWriteAction BiFunction<R, Continuation<?>, O> write
    ) {
        return new ReadWriteLock<>(read, write, true);
    }

    private static final Object STALE = new Object();

    private final BiFunction<I, Continuation<?>, R> myRead;
    private final BiFunction<R, Continuation<?>, O> myWrite;
    private final boolean myNonBlocking;
    private final boolean myAheadOfInput;

    private ReadWriteLock(BiFunction<I, Continuation<?>, R> read, BiFunction<R, Continuation<?>, O> write) {
        this(read, write, false, false);
    }

    private ReadWriteLock(BiFunction<I, Continuation<?>, R> read, BiFunction<R, Continuation<?>, O> write, boolean nonBlocking) {
        this(read, write, nonBlocking, false);
    }

    private ReadWriteLock(
        BiFunction<I, Continuation<?>, R> read,
        BiFunction<R, Continuation<?>, O> write,
        boolean nonBlocking,
        boolean aheadOfInput
    ) {
        myRead = read;
        myWrite = write;
        myNonBlocking = nonBlocking;
        myAheadOfInput = aheadOfInput;
    }

    /**
     * The write part does not wait for a pause in the input of a UI thread - for a short write the input itself waits for.
     *
     * @see ApplicationEx#runAheadOfInput
     */
    public ReadWriteLock<I, R, O> aheadOfInput() {
        return new ReadWriteLock<>(myRead, myWrite, myNonBlocking, true);
    }

    @Override
    protected @Nullable O execute(@Nullable I input, Continuation<?> continuation) {
        UIAccess.assetIsNotUIThread();
        ApplicationEx application = (ApplicationEx) Objects.requireNonNull(continuation.getConfiguration(Application.KEY), "Application required");
        if (application.isWriteThread()) {
            return readAndWrite(application, input, continuation);
        }
        if (myNonBlocking) {
            return readNonBlockingAndWrite(application, input, continuation);
        }
        return CompletableFuture.supplyAsync(() -> readAndWrite(application, input, continuation), application.getWriteExecutor()).join();
    }

    @SuppressWarnings("NullAway")
    private @Nullable O readAndWrite(ApplicationEx application, @Nullable I input, Continuation<?> continuation) {
        //noinspection RequiredXAction
        return inputPolicy(application, () -> application.runReadAndWriteAction(
            ReadWriteLock.class,
            () -> myRead.apply(input, continuation),
            result -> continuation.isFinished() ? null : myWrite.apply(result, continuation)
        ));
    }

    private <T extends @Nullable Object> T inputPolicy(ApplicationEx application, Supplier<T> action) {
        return myAheadOfInput ? application.runAheadOfInput(action) : action.get();
    }

    @SuppressWarnings({"NullAway", "unchecked"})
    private @Nullable O readNonBlockingAndWrite(ApplicationEx application, @Nullable I input, Continuation<?> continuation) {
        while (!continuation.isFinished()) {
            SimpleReference<R> result = new SimpleReference<>();
            AtomicLong epoch = new AtomicLong();
            //noinspection RequiredXAction
            boolean read = ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> {
                epoch.set(application.getWriteEpoch());
                result.set(myRead.apply(input, continuation));
            });
            if (!read) {
                ProgressIndicatorUtils.yieldToPendingWriteActions();
                continue;
            }
            if (continuation.isFinished()) {
                return null;
            }

            Object written = CompletableFuture.supplyAsync(
                () -> inputPolicy(application, () -> writeIfUnchanged(application, epoch.get(), result.get(), continuation)),
                application.getWriteExecutor()
            ).join();
            if (written != STALE) {
                return (O) written;
            }
        }
        return null;
    }

    @SuppressWarnings("NullAway")
    private @Nullable Object writeIfUnchanged(ApplicationEx application, long epoch, R value, Continuation<?> continuation) {
        ApplicationWithIntentWriteLock writeLockApplication = (ApplicationWithIntentWriteLock) application;
        writeLockApplication.acquireWriteIntentLock(ReadWriteLock.class.getName());
        try {
            if (application.getWriteEpoch() != epoch) {
                return STALE;
            }
            //noinspection RequiredXAction
            return writeLockApplication.runWriteAction((Supplier<O>) () -> myWrite.apply(value, continuation));
        }
        finally {
            writeLockApplication.releaseWriteIntentLock();
        }
    }
}
