// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.project;

import com.intellij.ide.caches.FileContent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Consumer;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheUpdateRunner {
  private static final Logger LOG = Logger.getInstance(CacheUpdateRunner.class);
  private static final Key<Boolean> FAILED_TO_INDEX = Key.create("FAILED_TO_INDEX");
  private static final int PROC_COUNT = Runtime.getRuntime().availableProcessors();
  public static final int DEFAULT_MAX_INDEXER_THREADS = 4;

  public static void processFiles(@Nonnull ProgressIndicator indicator, @Nonnull Collection<VirtualFile> files, @Nonnull Project project, @Nonnull Consumer<? super FileContent> processor) {
    indicator.checkCanceled();
    final FileContentQueue queue = new FileContentQueue(project, files, indicator);
    final double total = files.size();
    queue.startLoading();

    indicator.setIndeterminate(false);

    ProgressUpdater progressUpdater = new ProgressUpdater() {
      final Set<VirtualFile> myFilesBeingProcessed = new HashSet<>();
      final AtomicInteger myNumberOfFilesProcessed = new AtomicInteger();

      @Override
      public void processingStarted(@Nonnull VirtualFile virtualFile) {
        indicator.checkCanceled();
        boolean added;
        synchronized (myFilesBeingProcessed) {
          added = myFilesBeingProcessed.add(virtualFile);
        }
        if (added) {
          indicator.setFraction(myNumberOfFilesProcessed.incrementAndGet() / total);

          VirtualFile parent = virtualFile.getParent();
          if (parent != null) indicator.setText2(parent.getPresentableUrl());
        }
      }

      @Override
      public void processingSuccessfullyFinished(@Nonnull VirtualFile virtualFile) {
        synchronized (myFilesBeingProcessed) {
          boolean removed = myFilesBeingProcessed.remove(virtualFile);
          assert removed;
        }
      }
    };

    while (!project.isDisposed()) {
      indicator.checkCanceled();
      if (processSomeFilesWhileUserIsInactive(queue, progressUpdater, indicator, project, processor)) {
        break;
      }
    }

    if (project.isDisposed()) {
      indicator.cancel();
      indicator.checkCanceled();
    }
  }

  interface ProgressUpdater {
    void processingStarted(@Nonnull VirtualFile file);

    void processingSuccessfullyFinished(@Nonnull VirtualFile file);
  }

  private static boolean processSomeFilesWhileUserIsInactive(@Nonnull FileContentQueue queue,
                                                             @Nonnull ProgressUpdater progressUpdater,
                                                             @Nonnull ProgressIndicator suspendableIndicator,
                                                             @Nonnull Project project,
                                                             @Nonnull Consumer<? super FileContent> fileProcessor) {
    final ProgressIndicatorBase innerIndicator = new ProgressIndicatorBase() {
      @Override
      protected boolean isCancelable() {
        return true; // the inner indicator must be always cancelable
      }
    };
    final ApplicationListener canceller = new ApplicationListener() {
      @Override
      public void beforeWriteActionStart(@Nonnull Object action) {
        innerIndicator.cancel();
      }
    };
    final Application application = ApplicationManager.getApplication();
    Disposable listenerDisposable = Disposable.newDisposable();
    application.invokeAndWait(() -> application.addApplicationListener(canceller, listenerDisposable), ModalityState.any());

    final AtomicBoolean isFinished = new AtomicBoolean();
    try {
      int threadsCount = indexingThreadCount();
      if (threadsCount == 1 || application.isWriteAccessAllowed()) {
        Runnable process = createRunnable(project, queue, progressUpdater, suspendableIndicator, innerIndicator, isFinished, fileProcessor);
        ProgressManager.getInstance().runProcess(process, innerIndicator);
      }
      else {
        AtomicBoolean[] finishedRefs = new AtomicBoolean[threadsCount];
        Future<?>[] futures = new Future<?>[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
          AtomicBoolean localFinished = new AtomicBoolean();
          finishedRefs[i] = localFinished;
          Runnable process = createRunnable(project, queue, progressUpdater, suspendableIndicator, innerIndicator, localFinished, fileProcessor);
          futures[i] = application.executeOnPooledThread(process);
        }
        isFinished.set(waitForAll(finishedRefs, futures));
      }
    }
    finally {
      Disposer.dispose(listenerDisposable);
    }

    return isFinished.get();
  }

  @Nonnull
  private static Runnable createRunnable(@Nonnull Project project,
                                         @Nonnull FileContentQueue queue,
                                         @Nonnull ProgressUpdater progressUpdater,
                                         @Nonnull ProgressIndicator suspendableIndicator,
                                         @Nonnull ProgressIndicatorBase innerIndicator,
                                         @Nonnull AtomicBoolean isFinished,
                                         @Nonnull Consumer<? super FileContent> fileProcessor) {
    return ConcurrencyUtil.underThreadNameRunnable("Indexing", new MyRunnable(innerIndicator, suspendableIndicator, queue, isFinished, progressUpdater, project, fileProcessor));
  }

  public static int indexingThreadCount() {
    int threadsCount = Registry.intValue("caches.indexerThreadsCount");
    if (threadsCount <= 0) {
      int coresToLeaveForOtherActivity = ApplicationManager.getApplication().isCommandLine() ? 0 : 1;
      threadsCount = Math.max(1, Math.min(PROC_COUNT - coresToLeaveForOtherActivity, DEFAULT_MAX_INDEXER_THREADS));
    }
    return threadsCount;
  }

  private static boolean waitForAll(@Nonnull AtomicBoolean[] finishedRefs, @Nonnull Future<?>[] futures) {
    assert !ApplicationManager.getApplication().isWriteAccessAllowed();
    try {
      for (Future<?> future : futures) {
        ProgressIndicatorUtils.awaitWithCheckCanceled(future);
      }

      boolean allFinished = true;
      for (AtomicBoolean ref : finishedRefs) {
        if (!ref.get()) {
          allFinished = false;
          break;
        }
      }
      return allFinished;
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable throwable) {
      LOG.error(throwable);
    }
    return false;
  }

  private static class MyRunnable implements Runnable {
    private final ProgressIndicatorBase myInnerIndicator;
    private final ProgressIndicator mySuspendableIndicator;
    private final FileContentQueue myQueue;
    private final AtomicBoolean myFinished;
    private final ProgressUpdater myProgressUpdater;
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final Consumer<? super FileContent> myProcessor;

    MyRunnable(@Nonnull ProgressIndicatorBase innerIndicator,
               @Nonnull ProgressIndicator suspendableIndicator,
               @Nonnull FileContentQueue queue,
               @Nonnull AtomicBoolean finished,
               @Nonnull ProgressUpdater progressUpdater,
               @Nonnull Project project,
               @Nonnull Consumer<? super FileContent> fileProcessor) {
      myInnerIndicator = innerIndicator;
      mySuspendableIndicator = suspendableIndicator;
      myQueue = queue;
      myFinished = finished;
      myProgressUpdater = progressUpdater;
      myProject = project;
      myProcessor = fileProcessor;
    }

    @Override
    public void run() {
      while (true) {
        if (myProject.isDisposedOrDisposeInProgress() || myInnerIndicator.isCanceled()) {
          return;
        }

        try {
          mySuspendableIndicator.checkCanceled();

          final FileContent fileContent = myQueue.take(myInnerIndicator);
          if (fileContent == null) {
            myFinished.set(true);
            return;
          }

          final Runnable action = () -> {
            myInnerIndicator.checkCanceled();
            if (!myProject.isDisposedOrDisposeInProgress()) {
              final VirtualFile file = fileContent.getVirtualFile();
              try {
                myProgressUpdater.processingStarted(file);
                if (!file.isDirectory() && !Boolean.TRUE.equals(file.getUserData(FAILED_TO_INDEX))) {
                  myProcessor.consume(fileContent);
                }
                myProgressUpdater.processingSuccessfullyFinished(file);
              }
              catch (ProcessCanceledException e) {
                throw e;
              }
              catch (Throwable e) {
                handleIndexingException(file, e);
              }
            }
          };
          try {
            ProgressManager.getInstance().runProcess(() -> {
              // in wait methods we don't want to deadlock by grabbing write lock (or having it in queue) and trying to run read action in separate thread
              ApplicationEx app = ApplicationManagerEx.getApplicationEx();
              if (app.isDisposedOrDisposeInProgress() || !app.tryRunReadAction(action)) {
                throw new ProcessCanceledException();
              }
            }, ProgressWrapper.wrap(myInnerIndicator));
          }
          catch (ProcessCanceledException e) {
            myQueue.pushBack(fileContent);
            return;
          }
          finally {
            myQueue.release(fileContent);
          }
        }
        catch (ProcessCanceledException e) {
          return;
        }
      }
    }

    private static void handleIndexingException(@Nonnull VirtualFile file, @Nonnull Throwable e) {
      file.putUserData(FAILED_TO_INDEX, Boolean.TRUE);
      LOG.error("Error while indexing " + file.getPresentableUrl() + "\n" + "To reindex this file IDEA has to be restarted", e);
    }
  }
}