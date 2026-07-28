// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.ApplicationManagerEx;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.internal.SensitiveProgressWrapper;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class CacheUpdateRunner {
    private static final Logger LOG = Logger.getInstance(CacheUpdateRunner.class);
    private static final Key<Boolean> FAILED_TO_INDEX = Key.create("FAILED_TO_INDEX");
    private static final int PROC_COUNT = Runtime.getRuntime().availableProcessors();

    private static final long MIN_CONTENTION_BACKOFF_NANOS = 100_000;
    private static final long MAX_CONTENTION_BACKOFF_NANOS = 5_000_000;

    public static void processFiles(
        ProgressIndicator indicator,
        Collection<VirtualFile> files,
        Project project,
        Consumer<? super IndexFileContent> processor
    ) {
        indicator.checkCanceled();

        Application application = ApplicationManager.getApplication();
        int workerCount = indexingThreadCount();
        boolean inline = workerCount == 1 || application.isWriteAccessAllowed();

        FileContentQueue queue = new FileContentQueue(project, files, indicator, inline ? 1 : workerCount);
        queue.startLoading();

        indicator.setIndeterminate(false);
        ProgressUpdater progressUpdater = new ProgressUpdater(indicator, files.size());

        if (inline) {
            newWorker(project, queue, progressUpdater, indicator, processor).run();
        }
        else {
            List<Future<?>> futures = new ArrayList<>(workerCount);
            for (int i = 0; i < workerCount; i++) {
                futures.add(application.executeOnPooledThread(newWorker(project, queue, progressUpdater, indicator, processor)));
            }
            waitForAll(futures);
        }

        if (project.isDisposed()) {
            indicator.cancel();
            indicator.checkCanceled();
        }
    }

    private static Runnable newWorker(
        Project project,
        FileContentQueue queue,
        ProgressUpdater progressUpdater,
        ProgressIndicator indicator,
        Consumer<? super IndexFileContent> processor
    ) {
        return ConcurrencyUtil.underThreadNameRunnable("Indexing", new IndexingWorker(project, queue, progressUpdater, indicator, processor));
    }

    public static int indexingThreadCount() {
        int threadsCount = Registry.intValue("caches.indexerThreadsCount");
        if (threadsCount > 0) {
            return threadsCount;
        }
        int coresToLeaveForOtherActivity = ApplicationManager.getApplication().isCommandLine() ? 0 : 1;
        return Math.max(1, PROC_COUNT - coresToLeaveForOtherActivity);
    }

    public static int scanningThreadCount() {
        return Math.max(1, Math.min(indexingThreadCount(), 4));
    }

    private static void waitForAll(List<Future<?>> futures) {
        assert !ApplicationManager.getApplication().isWriteAccessAllowed();
        for (Future<?> future : futures) {
            ProgressIndicatorUtils.awaitWithCheckCanceled(future);
        }
    }

    private static class ProgressUpdater {
        private final ProgressIndicator myIndicator;
        private final double myTotal;
        private final AtomicInteger myProcessed = new AtomicInteger();

        ProgressUpdater(ProgressIndicator indicator, int total) {
            myIndicator = indicator;
            myTotal = total;
        }

        void processed(VirtualFile file) {
            myIndicator.setFraction(myProcessed.incrementAndGet() / myTotal);

            VirtualFile parent = file.getParent();
            if (parent != null) {
                myIndicator.setText2(parent.getPresentableUrl());
            }
        }
    }

    private static class IndexingWorker implements Runnable {
        private final Project myProject;
        private final FileContentQueue myQueue;
        private final ProgressUpdater myProgressUpdater;
        private final ProgressIndicator myIndicator;
        private final Consumer<? super IndexFileContent> myProcessor;
        private final ApplicationEx myApplication = ApplicationManagerEx.getApplicationEx();

        IndexingWorker(
            Project project,
            FileContentQueue queue,
            ProgressUpdater progressUpdater,
            ProgressIndicator indicator,
            Consumer<? super IndexFileContent> processor
        ) {
            myProject = project;
            myQueue = queue;
            myProgressUpdater = progressUpdater;
            myIndicator = indicator;
            myProcessor = processor;
        }

        @Override
        public void run() {
            while (!myProject.isDisposedOrDisposeInProgress() && !myApplication.isDisposedOrDisposeInProgress()) {
                IndexFileContent fileContent;
                try {
                    myIndicator.checkCanceled();

                    fileContent = myQueue.take(myIndicator);
                    if (fileContent == null) {
                        return;
                    }
                }
                catch (ProcessCanceledException e) {
                    return;
                }

                try {
                    indexUntilDone(fileContent);
                }
                catch (ProcessCanceledException e) {
                    return;
                }
                finally {
                    myQueue.release(fileContent);
                }
            }
        }

        private void indexUntilDone(IndexFileContent fileContent) {
            VirtualFile file = fileContent.getVirtualFile();
            if (file.isDirectory() || Boolean.TRUE.equals(file.getUserData(FAILED_TO_INDEX))) {
                myProgressUpdater.processed(file);
                return;
            }

            Runnable action = () -> {
                try {
                    myProcessor.accept(fileContent);
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Throwable e) {
                    handleIndexingException(file, e);
                }
            };

            if (myApplication.isWriteAccessAllowed() || myApplication.isDispatchThread()) {
                action.run();
                myProgressUpdater.processed(file);
                return;
            }

            long backoffNanos = MIN_CONTENTION_BACKOFF_NANOS;
            while (true) {
                myIndicator.checkCanceled();
                if (myProject.isDisposedOrDisposeInProgress()) {
                    throw new ProcessCanceledException();
                }

                if (!isWriting() && ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(action, new AttemptIndicator(myIndicator))) {
                    myProgressUpdater.processed(file);
                    return;
                }

                LockSupport.parkNanos(backoffNanos);
                backoffNanos = Math.min(backoffNanos * 2, MAX_CONTENTION_BACKOFF_NANOS);
            }
        }

        private boolean isWriting() {
            return myApplication.isWriteActionPending() || myApplication.isWriteActionInProgress();
        }

        private static void handleIndexingException(VirtualFile file, Throwable e) {
            file.putUserData(FAILED_TO_INDEX, Boolean.TRUE);
            LOG.error("Error while indexing " + file.getPresentableUrl() + "\n" + "To reindex this file IDE has to be restarted", e);
        }
    }

    private static class AttemptIndicator extends SensitiveProgressWrapper {
        AttemptIndicator(ProgressIndicator original) {
            super(original);
            dontStartActivity();
        }
    }
}
