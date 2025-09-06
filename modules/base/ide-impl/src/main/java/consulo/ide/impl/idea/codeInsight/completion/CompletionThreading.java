// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.application.ApplicationManager;
import consulo.application.internal.ApplicationManagerEx;
import consulo.application.internal.ProgressWrapper;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.Semaphore;
import consulo.application.util.function.Computable;
import consulo.component.ProcessCanceledException;
import consulo.language.editor.completion.CompletionResult;
import consulo.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * @author peter
 */
interface CompletionThreading {

    Future<?> startThread(ProgressIndicator progressIndicator, Runnable runnable);

    WeighingDelegate delegateWeighing(CompletionProgressIndicator indicator);
}

interface WeighingDelegate extends Consumer<CompletionResult> {
    void waitFor();
}

class SyncCompletion extends CompletionThreadingBase {
    private final List<CompletionResult> myBatchList = new ArrayList<>();

    @Override
    public Future<?> startThread(ProgressIndicator progressIndicator, Runnable runnable) {
        ProgressManager.getInstance().runProcess(runnable, progressIndicator);

        CompletableFuture<Object> result = new CompletableFuture<>();
        result.complete(true);
        return result;
    }

    @Override
    public WeighingDelegate delegateWeighing(final CompletionProgressIndicator indicator) {
        return new WeighingDelegate() {
            @Override
            public void waitFor() {
                indicator.addDelayedMiddleMatches();
            }

            @Override
            public void accept(CompletionResult result) {
                if (ourIsInBatchUpdate.get().booleanValue()) {
                    myBatchList.add(result);
                }
                else {
                    indicator.addItem(result);
                }
            }
        };
    }

    @Override
    protected void flushBatchResult(CompletionProgressIndicator indicator) {
        try {
            indicator.withSingleUpdate(() -> {
                for (CompletionResult result : myBatchList) {
                    indicator.addItem(result);
                }
            });
        }
        finally {
            myBatchList.clear();
        }
    }
}

class AsyncCompletion extends CompletionThreadingBase {
    private static final Logger LOG = Logger.getInstance(AsyncCompletion.class);
    private final ArrayList<CompletionResult> myBatchList = new ArrayList<>();
    private final LinkedBlockingQueue<Computable<Boolean>> myQueue = new LinkedBlockingQueue<>();

    @Override
    public Future<?> startThread(ProgressIndicator progressIndicator, Runnable runnable) {
        Semaphore startSemaphore = new Semaphore();
        startSemaphore.down();
        Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(() -> ProgressManager.getInstance().runProcess(() -> {
            try {
                startSemaphore.up();
                ProgressManager.checkCanceled();
                runnable.run();
            }
            catch (ProcessCanceledException ignored) {
            }
        }, progressIndicator));
        startSemaphore.waitFor();
        return future;
    }

    @Override
    public WeighingDelegate delegateWeighing(final CompletionProgressIndicator indicator) {

        class WeighItems implements Runnable {
            @Override
            public void run() {
                try {
                    while (true) {
                        Computable<Boolean> next = myQueue.poll(30, TimeUnit.MILLISECONDS);
                        if (next != null && !next.compute()) {
                            tryReadOrCancel(indicator, () -> indicator.addDelayedMiddleMatches());
                            return;
                        }
                        indicator.checkCanceled();
                    }
                }
                catch (InterruptedException e) {
                    LOG.error(e);
                }
            }
        }

        final Future<?> future = startThread(ProgressWrapper.wrap(indicator), new WeighItems());
        return new WeighingDelegate() {
            @Override
            public void waitFor() {
                myQueue.offer(new Computable.PredefinedValueComputable<>(false));
                try {
                    future.get();
                }
                catch (InterruptedException | ExecutionException e) {
                    LOG.error(e);
                }
            }

            @Override
            public void accept(CompletionResult result) {
                if (ourIsInBatchUpdate.get().booleanValue()) {
                    myBatchList.add(result);
                }
                else {
                    myQueue.offer(() -> {
                        tryReadOrCancel(indicator, () -> indicator.addItem(result));
                        return true;
                    });
                }
            }
        };
    }

    @Override
    protected void flushBatchResult(CompletionProgressIndicator indicator) {
        ArrayList<CompletionResult> batchListCopy = new ArrayList<>(myBatchList);
        myBatchList.clear();

        myQueue.offer(() -> {
            tryReadOrCancel(indicator, () -> indicator.withSingleUpdate(() -> {
                for (CompletionResult result : batchListCopy) {
                    indicator.addItem(result);
                }
            }));
            return true;
        });
    }

    static void tryReadOrCancel(ProgressIndicator indicator, Runnable runnable) {
        if (!ApplicationManagerEx.getApplicationEx().tryRunReadAction(() -> {
            indicator.checkCanceled();
            runnable.run();
        })) {
            indicator.cancel();
            indicator.checkCanceled();
        }
    }
}

