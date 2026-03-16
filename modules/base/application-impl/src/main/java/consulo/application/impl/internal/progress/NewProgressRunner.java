// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.progress;

import consulo.application.Application;
import consulo.application.internal.BlockingProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.ClientId;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.ref.SimpleReference;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NewProgressRunner<R> {
    
    private final Function<? super ProgressIndicator, ? extends R> myComputation;

    private final boolean isModal;

    
    private final CompletableFuture<? extends ProgressIndicator> myProgressIndicatorFuture;

    public NewProgressRunner(Function<? super ProgressIndicator, ? extends R> computation,
                             boolean modal,
                             CompletableFuture<? extends ProgressIndicator> progressIndicatorFuture) {
        myComputation = ClientId.decorateFunction(computation);
        isModal = modal;
        myProgressIndicatorFuture = progressIndicatorFuture;
    }

    
    public CompletableFuture<R> submit(Application application) {
        if (application.isWriteAccessAllowed()) {
            throw new IllegalArgumentException("Can't start task under write lock");
        }

        if (!application.isDispatchThread() && application.isReadAccessAllowed()) {
            throw new IllegalArgumentException("Can't start task under read lock");
        }

        Supplier<R> onThreadCallable = () -> {
            SimpleReference<R> result = SimpleReference.create();

            // runProcess handles starting/stopping progress and setting thread's current progress
            ProgressIndicator progressIndicator;
            try {
                progressIndicator = myProgressIndicatorFuture.join();
            }
            catch (Throwable e) {
                throw new RuntimeException("Can't get progress", e);
            }
            //noinspection ConstantConditions
            if (progressIndicator == null) {
                throw new IllegalStateException("Expected not-null progress indicator but got null from " + myProgressIndicatorFuture);
            }

            ProgressManager.getInstance().runProcess(() -> result.set(myComputation.apply(progressIndicator)), progressIndicator);
            return result.get();
        };

        return exec(myProgressIndicatorFuture, onThreadCallable);
    }

    
    private CompletableFuture<R> exec(CompletableFuture<? extends ProgressIndicator> progressFuture,
                                      Supplier<R> onThreadCallable) {
        CompletableFuture<R> taskFuture = launchTask(onThreadCallable);
        CompletableFuture<R> resultFuture;

        if (isModal) {
            // Running task with blocking EDT event pumping has the following contract in test mode:
            //   if a random EDT event processed by blockingPI fails with an exception, the event pumping
            //   is stopped and the submitted task fails with respective exception.
            // The task submitted to e.g. POOLED thread might not be able to finish at all because it requires invoke&waits,
            //   but EDT is broken due an exception. Hence, initial task should be completed exceptionally
            CompletableFuture<Void> blockingRunFuture = progressFuture.thenAccept(progressIndicator -> {
                if (progressIndicator instanceof BlockingProgressIndicator) {
                    ((BlockingProgressIndicator) progressIndicator).startBlocking(EmptyRunnable.getInstance(), taskFuture);
                }
                else {
                    Logger.getInstance(NewProgressRunner.class).warn("Can't go modal without BlockingProgressIndicator");
                }
            }).exceptionally(throwable -> {
                taskFuture.completeExceptionally(throwable);
                return null;
            });
            // `startBlocking` might throw unrelated exceptions execute the submitted task so potential failure should be handled.
            // Relates to testMode-ish execution: unhandled exceptions on event pumping are `LOG.error`-ed, hence throw exceptions in tests.
            resultFuture = taskFuture.thenCombine(blockingRunFuture, (r, __) -> r);
        }
        else {
            resultFuture = taskFuture;
        }

        return resultFuture;
    }

    
    private CompletableFuture<R> launchTask(Supplier<R> callable) {
        return CompletableFuture.supplyAsync(callable, AppExecutorUtil.getAppExecutorService());
    }
}
