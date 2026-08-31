// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.virtualFileSystem.InvalidVirtualFileAccessException;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author peter
 */
public class FileContentQueue {
    private static final Logger LOG = Logger.getInstance(FileContentQueue.class);

    private static final long SOFT_LIMIT_OF_BYTES_PER_WORKER = 4 * 1024 * 1024;
    private static final int MAX_LOADER_THREADS = 4;
    private static final long BUDGET_WAIT_MILLIS = 100;

    private final Project myProject;
    private final ProgressIndicator myProgressIndicator;

    private final @Nullable BlockingQueue<VirtualFile> myFilesQueue;
    private final BlockingQueue<IndexFileContent> myLoadedContents = new LinkedBlockingQueue<>();
    private final AtomicInteger myContentsToLoad = new AtomicInteger();

    private final long mySoftLimitOfBytes;
    private final int myLoaderThreads;

    private final AtomicLong myBytesInMemory = new AtomicLong();
    private final Lock myBudgetLock = new ReentrantLock();
    private final Condition myBudgetReleased = myBudgetLock.newCondition();

    FileContentQueue(Project project, Collection<VirtualFile> files, ProgressIndicator indicator, int workerCount) {
        myProject = project;
        myProgressIndicator = indicator;
        int numberOfFiles = files.size();
        myContentsToLoad.set(numberOfFiles);
        // ABQ is more memory efficient for significant number of files (e.g. 500K)
        myFilesQueue = numberOfFiles > 0 ? new ArrayBlockingQueue<>(numberOfFiles, false, files) : null;
        mySoftLimitOfBytes = SOFT_LIMIT_OF_BYTES_PER_WORKER * workerCount;
        myLoaderThreads = Math.max(1, Math.min(workerCount, MAX_LOADER_THREADS));
    }

    public void startLoading() {
        if (myContentsToLoad.get() == 0) {
            return;
        }

        for (int i = 0; i < myLoaderThreads; i++) {
            myProject.getApplication()
                .executeOnPooledThread(ConcurrencyUtil.underThreadNameRunnable("Indexing content loader", this::loadUntilDrained));
        }
    }

    private void loadUntilDrained() {
        try {
            while (!myProgressIndicator.isCanceled() && !myProject.isDisposedOrDisposeInProgress()) {
                if (!awaitBudget() || !loadNextContent()) {
                    return;
                }
            }
        }
        catch (ProcessCanceledException ignored) {
        }
    }

    private boolean awaitBudget() {
        myBudgetLock.lock();
        try {
            while (myBytesInMemory.get() > mySoftLimitOfBytes) {
                if (myProgressIndicator.isCanceled()) {
                    return false;
                }
                myBudgetReleased.await(BUDGET_WAIT_MILLIS, TimeUnit.MILLISECONDS);
            }
            return true;
        }
        catch (InterruptedException e) {
            return false;
        }
        finally {
            myBudgetLock.unlock();
        }
    }

    private boolean loadNextContent() {
        // Contract: if file is taken from myFilesQueue then it will be loaded to myLoadedContents and myContentsToLoad will be decremented
        VirtualFile file = myFilesQueue == null ? null : myFilesQueue.poll();
        if (file == null) {
            return false;
        }

        try {
            IndexFileContent content = new IndexFileContent(file);
            if (!isValidFile(file) || !doLoadContent(content)) {
                content.setEmptyContent();
            }
            myBytesInMemory.addAndGet(content.getLength());
            myLoadedContents.offer(content);
            return true;
        }
        finally {
            myContentsToLoad.decrementAndGet();
        }
    }

    private static boolean isValidFile(VirtualFile file) {
        return file.isValid() && !file.isDirectory() && !file.is(VFileProperty.SPECIAL) && !VirtualFileUtil.isBrokenLink(file);
    }

    @SuppressWarnings("InstanceofCatchParameter")
    private boolean doLoadContent(IndexFileContent content) {
        try {
            // Reads the content bytes and caches them.
            // hint at the current project to avoid expensive read action in ProjectLocatorImpl
            ProjectLocator.computeWithPreferredProject(content.getVirtualFile(), myProject, () -> content.getBytes());

            return true;
        }
        catch (Throwable e) {
            if (e instanceof IOException || e instanceof InvalidVirtualFileAccessException) {
                if (e instanceof FileNotFoundException) {
                    LOG.debug(e); // it is possible to not observe file system change until refresh finish, we handle missed file properly anyway
                }
                else {
                    LOG.info(e);
                }
            }
            else {
                LOG.error(e);
            }

            return false;
        }
    }

    public @Nullable IndexFileContent take(ProgressIndicator indicator) throws ProcessCanceledException {
        while (true) {
            indicator.checkCanceled();

            int remainingToLoad = myContentsToLoad.get();

            IndexFileContent content = myLoadedContents.poll();
            if (content != null) {
                return content;
            }

            if (remainingToLoad == 0) {
                return null;
            }

            if (!loadNextContent()) {
                try {
                    content = myLoadedContents.poll(BUDGET_WAIT_MILLIS, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e) {
                    throw new ProcessCanceledException(e);
                }

                if (content != null) {
                    return content;
                }
            }
        }
    }

    public void release(IndexFileContent content) {
        myBytesInMemory.addAndGet(-content.getLength());

        myBudgetLock.lock();
        try {
            myBudgetReleased.signalAll();
        }
        finally {
            myBudgetLock.unlock();
        }
    }
}
