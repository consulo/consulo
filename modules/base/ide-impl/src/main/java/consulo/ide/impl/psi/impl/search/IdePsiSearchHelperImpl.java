/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.psi.impl.search;

import consulo.annotation.component.ServiceImpl;
import consulo.application.event.ApplicationListener;
import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.internal.SensitiveProgressWrapper;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.JobLauncher;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.cacheBuilder.CacheManager;
import consulo.language.impl.internal.psi.search.PsiSearchHelperImpl;
import consulo.language.psi.PsiManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.EmptyRunnable;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2023-11-08
 */
@Singleton
@ServiceImpl
public class IdePsiSearchHelperImpl extends PsiSearchHelperImpl {
  @Inject
  public IdePsiSearchHelperImpl(PsiManager manager,
                                DumbService dumbService,
                                CacheManager cacheManager) {
    super(manager, dumbService, cacheManager);
  }

  // Tries to run {@code localProcessor} for each file in {@code files} concurrently on ForkJoinPool.
  // When encounters write action request, stops all threads, waits for write action to finish and re-starts all threads again.
  // {@code localProcessor} must be as idempotent as possible.
  @Override
  public boolean processFilesConcurrentlyDespiteWriteActions(@Nonnull Project project,
                                                             @Nonnull List<? extends VirtualFile> files,
                                                             @Nonnull final ProgressIndicator progress,
                                                             @Nonnull AtomicBoolean stopped,
                                                             @Nonnull final Predicate<? super VirtualFile> localProcessor) {
    ApplicationEx app = (ApplicationEx)project.getApplication();
    if (!app.isDispatchThread()) {
      CoreProgressManager.assertUnderProgress(progress);
    }
    List<VirtualFile> processedFiles = Collections.synchronizedList(new ArrayList<>(files.size()));
    while (true) {
      ProgressManager.checkCanceled();
      ProgressIndicator wrapper = new SensitiveProgressWrapper(progress);
      ApplicationListener listener = new ApplicationListener() {
        @Override
        public void beforeWriteActionStart(@Nonnull Object action) {
          wrapper.cancel();
        }
      };
      processedFiles.clear();
      Disposable disposable = Disposable.newDisposable();
      app.addApplicationListener(listener, disposable);
      boolean processorCanceled = false;
      try {
        if (app.isWriteAccessAllowed() || app.isReadAccessAllowed() && app.isWriteActionPending()) {
          // no point in processing in separate threads - they are doomed to fail to obtain read action anyway
          // do not wrap in impatient reader because every read action inside would trigger AU.CRRAE
          processorCanceled = !ContainerUtil.process(files, localProcessor);
          if (processorCanceled) {
            stopped.set(true);
          }
          processedFiles.addAll(files);
        }
        else if (app.isWriteActionPending()) {
          // we don't have read action now so wait for write action to complete
        }
        else {
          AtomicBoolean someTaskFailed = new AtomicBoolean();
          Predicate<VirtualFile> processor = vfile -> {
            ProgressManager.checkCanceled();
            // optimisation: avoid unnecessary processing if it's doomed to fail because some other task has failed already,
            // and bail out of fork/join task as soon as possible
            if (someTaskFailed.get()) {
              return false;
            }
            try {
              // wrap in unconditional impatient reader to bail early at write action start,
              // regardless of whether was called from highlighting (already impatient-wrapped) or Find Usages action
              app.executeByImpatientReader(() -> {
                if (localProcessor.test(vfile)) {
                  processedFiles.add(vfile);
                }
                else {
                  stopped.set(true);
                }
              });
            }
            catch (ProcessCanceledException e) {
              someTaskFailed.set(true);
              throw e;
            }
            return !stopped.get();
          };
          // try to run parallel read actions but fail as soon as possible
          try {
            JobLauncher.getInstance().invokeConcurrentlyUnderProgress(files, wrapper, processor);
            processorCanceled = stopped.get();
          }
          catch (ProcessCanceledException e) {
            // we can be interrupted by wrapper (means write action is about to start) or by genuine exception in progress
            progress.checkCanceled();
          }
        }
      }
      finally {
        Disposer.dispose(disposable);
      }
      if (processorCanceled) {
        return false;
      }

      if (processedFiles.size() == files.size()) {
        break;
      }
      // we failed to run read action in job launcher thread
      // run read action in our thread instead to wait for a write action to complete and resume parallel processing
      DumbService.getInstance(project).runReadActionInSmartMode(EmptyRunnable.getInstance());
      Set<VirtualFile> t = new HashSet<>(files);
      synchronized (processedFiles) {
        t.removeAll(processedFiles);
      }
      files = new ArrayList<>(t);
    }
    return true;
  }
}
