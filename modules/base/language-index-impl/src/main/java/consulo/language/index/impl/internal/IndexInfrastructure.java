// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.container.boot.ContainerPathManager;
import consulo.index.io.ID;
import consulo.language.index.impl.internal.stub.StubUpdatingIndex;
import consulo.language.psi.stub.StubIndexKey;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.PersistentFS;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author max
 */
@SuppressWarnings("HardCodedStringLiteral")
public class IndexInfrastructure {
  private static final String STUB_VERSIONS = ".versions";
  private static final String PERSISTENT_INDEX_DIRECTORY_NAME = ".persistent";
  private static final boolean ourDoParallelIndicesInitialization =
    SystemProperties.getBooleanProperty("idea.parallel.indices.initialization", false);
  public static final boolean ourDoAsyncIndicesInitialization =
    SystemProperties.getBooleanProperty("idea.async.indices.initialization", true);
  private static final ExecutorService ourGenesisExecutor =
    SequentialTaskExecutor.createSequentialApplicationPoolExecutor("IndexInfrastructure Pool");

  private IndexInfrastructure() {
  }

  
  public static File getVersionFile(ID<?, ?> indexName) {
    return new File(getIndexDirectory(indexName, true), indexName + ".ver");
  }

  
  public static File getStorageFile(ID<?, ?> indexName) {
    return new File(getIndexRootDir(indexName), indexName.getName());
  }

  
  public static File getInputIndexStorageFile(ID<?, ?> indexName) {
    return new File(getIndexRootDir(indexName), indexName + "_inputs");
  }

  
  public static File getIndexRootDir(ID<?, ?> indexName) {
    return getIndexDirectory(indexName, false);
  }

  public static File getPersistentIndexRoot() {
    File indexDir = new File(ContainerPathManager.get().getIndexRoot() + File.separator + PERSISTENT_INDEX_DIRECTORY_NAME);
    indexDir.mkdirs();
    return indexDir;
  }

  
  public static File getPersistentIndexRootDir(ID<?, ?> indexName) {
    return getIndexDirectory(indexName, false, PERSISTENT_INDEX_DIRECTORY_NAME);
  }

  
  private static File getIndexDirectory(ID<?, ?> indexName, boolean forVersion) {
    return getIndexDirectory(indexName, forVersion, "");
  }

  
  private static File getIndexDirectory(ID<?, ?> indexName, boolean forVersion, String relativePath) {
    String dirName = StringUtil.toLowerCase(indexName.getName());
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

  public static @Nullable VirtualFile findFileById(PersistentFS fs, int id) {
    return fs.findFileById(id);
  }

  public static @Nullable VirtualFile findFileByIdIfCached(PersistentFS fs, int id) {
    return fs.findFileByIdIfCached(id);
  }

  
  public static <T> Future<T> submitGenesisTask(Callable<T> action) {
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

    protected abstract void onThrowable(Throwable t);

    protected void addNestedInitializationTask(ThrowableRunnable<?> nestedInitializationTask) {
      myNestedInitializationTasks.add(nestedInitializationTask);
    }

    private void runParallelNestedInitializationTasks() throws InterruptedException {
      int numberOfTasksToExecute = myNestedInitializationTasks.size();
      if (numberOfTasksToExecute == 0) return;

      CountDownLatch proceedLatch = new CountDownLatch(numberOfTasksToExecute);

      if (ourDoParallelIndicesInitialization) {
        ExecutorService taskExecutor = AppExecutorUtil
          .createBoundedApplicationPoolExecutor("IndexInfrastructure.DataInitialization.RunParallelNestedInitializationTasks",
                                                PooledThreadExecutor.getInstance(), CacheUpdateRunner.indexingThreadCount());

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

    private void executeNestedInitializationTask(ThrowableRunnable<?> callable, CountDownLatch proceedLatch) {
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
    return true;
  }
}
