// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.util.indexing;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.CacheUpdateRunner;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.SystemProperties;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import org.jetbrains.ide.PooledThreadExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@SuppressWarnings("HardCodedStringLiteral")
public class IndexInfrastructure {
  private static final String STUB_VERSIONS = ".versions";
  private static final String PERSISTENT_INDEX_DIRECTORY_NAME = ".persistent";
  private static final boolean ourDoParallelIndicesInitialization = SystemProperties.getBooleanProperty("idea.parallel.indices.initialization", false);
  public static final boolean ourDoAsyncIndicesInitialization = SystemProperties.getBooleanProperty("idea.async.indices.initialization", true);
  private static final ExecutorService ourGenesisExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("IndexInfrastructure Pool");

  private IndexInfrastructure() {
  }

  @Nonnull
  public static File getVersionFile(@Nonnull ID<?, ?> indexName) {
    return new File(getIndexDirectory(indexName, true), indexName + ".ver");
  }

  @Nonnull
  public static File getStorageFile(@Nonnull ID<?, ?> indexName) {
    return new File(getIndexRootDir(indexName), indexName.getName());
  }

  @Nonnull
  public static File getInputIndexStorageFile(@Nonnull ID<?, ?> indexName) {
    return new File(getIndexRootDir(indexName), indexName + "_inputs");
  }

  @Nonnull
  public static File getIndexRootDir(@Nonnull ID<?, ?> indexName) {
    return getIndexDirectory(indexName, false);
  }

  public static File getPersistentIndexRoot() {
    File indexDir = new File(ContainerPathManager.get().getIndexRoot() + File.separator + PERSISTENT_INDEX_DIRECTORY_NAME);
    indexDir.mkdirs();
    return indexDir;
  }

  @Nonnull
  public static File getPersistentIndexRootDir(@Nonnull ID<?, ?> indexName) {
    return getIndexDirectory(indexName, false, PERSISTENT_INDEX_DIRECTORY_NAME);
  }

  @Nonnull
  private static File getIndexDirectory(@Nonnull ID<?, ?> indexName, boolean forVersion) {
    return getIndexDirectory(indexName, forVersion, "");
  }

  @Nonnull
  private static File getIndexDirectory(@Nonnull ID<?, ?> indexName, boolean forVersion, String relativePath) {
    final String dirName = StringUtil.toLowerCase(indexName.getName());
    File indexDir;

    if (indexName instanceof StubIndexKey) {
      // store StubIndices under StubUpdating index' root to ensure they are deleted
      // when StubUpdatingIndex version is changed
      indexDir = new File(getIndexDirectory(StubUpdatingIndex.INDEX_ID, false, relativePath), forVersion ? STUB_VERSIONS : dirName);
    }
    else {
      if (relativePath.length() > 0) relativePath = File.separator + relativePath;
      indexDir = new File(ContainerPathManager.get().getIndexRoot() + relativePath, dirName);
    }
    indexDir.mkdirs();
    return indexDir;
  }

  @Nullable
  public static VirtualFile findFileById(@Nonnull PersistentFS fs, final int id) {
    return fs.findFileById(id);
  }

  @Nullable
  public static VirtualFile findFileByIdIfCached(@Nonnull PersistentFS fs, final int id) {
    return fs.findFileByIdIfCached(id);
  }

  @Nonnull
  public static <T> Future<T> submitGenesisTask(@Nonnull Callable<T> action) {
    return ourGenesisExecutor.submit(action);
  }

  public abstract static class DataInitialization<T> implements Callable<T> {
    private final List<ThrowableRunnable<?>> myNestedInitializationTasks = new ArrayList<>();

    @Override
    public final T call() throws Exception {
      long started = System.nanoTime();
      try {
        prepare();
        runParallelNestedInitializationTasks();
        return finish();
      }
      finally {
        Logger.getInstance(getClass()).info("Initialization done: " + (System.nanoTime() - started) / 1000000);
      }
    }

    protected T finish() {
      return null;
    }

    protected void prepare() {
    }

    protected abstract void onThrowable(@Nonnull Throwable t);

    protected void addNestedInitializationTask(@Nonnull ThrowableRunnable<?> nestedInitializationTask) {
      myNestedInitializationTasks.add(nestedInitializationTask);
    }

    private void runParallelNestedInitializationTasks() throws InterruptedException {
      int numberOfTasksToExecute = myNestedInitializationTasks.size();
      if (numberOfTasksToExecute == 0) return;

      CountDownLatch proceedLatch = new CountDownLatch(numberOfTasksToExecute);

      if (ourDoParallelIndicesInitialization) {
        ExecutorService taskExecutor = AppExecutorUtil
                .createBoundedApplicationPoolExecutor("IndexInfrastructure.DataInitialization.RunParallelNestedInitializationTasks", PooledThreadExecutor.INSTANCE, CacheUpdateRunner.indexingThreadCount());

        for (ThrowableRunnable<?> callable : myNestedInitializationTasks) {
          taskExecutor.execute(() -> executeNestedInitializationTask(callable, proceedLatch));
        }

        proceedLatch.await();
        taskExecutor.shutdown();
      }
      else {
        for (ThrowableRunnable<?> callable : myNestedInitializationTasks) {
          executeNestedInitializationTask(callable, proceedLatch);
        }
      }
    }

    private void executeNestedInitializationTask(@Nonnull ThrowableRunnable<?> callable, CountDownLatch proceedLatch) {
      Application app = ApplicationManager.getApplication();
      try {
        // To correctly apply file removals in indices's shutdown hook we should process all initialization tasks
        // Todo: make processing removed files more robust because ignoring 'dispose in progress' delays application exit and
        // may cause memory leaks IDEA-183718, IDEA-169374,
        if (app.isDisposed() /*|| app.isDisposeInProgress()*/) return;
        callable.run();
      }
      catch (Throwable t) {
        onThrowable(t);
      }
      finally {
        proceedLatch.countDown();
      }
    }
  }

  public static boolean hasIndices() {
    return !SystemProperties.is("idea.skip.indices.initialization");
  }
}