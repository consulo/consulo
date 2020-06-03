package com.intellij.vcs.log.data;

import consulo.disposer.Disposable;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.Comparing;
import consulo.disposer.Disposer;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.CommitId;
import com.intellij.vcs.log.VcsLogProvider;
import com.intellij.vcs.log.VcsShortCommitDetails;
import com.intellij.vcs.log.data.index.IndexedDetails;
import com.intellij.vcs.log.data.index.VcsLogIndex;
import com.intellij.vcs.log.util.SequentialLimitedLifoExecutor;
import consulo.logging.Logger;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The DataGetter realizes the following pattern of getting some data (parametrized by {@code T}) from the VCS:
 * <ul>
 * <li>it tries to get it from the cache;</li>
 * <li>if it fails, it tries to get it from the VCS, and additionally loads several commits around the requested one,
 * to avoid querying the VCS if user investigates details of nearby commits.</li>
 * <li>The loading happens asynchronously: a fake {@link LoadingDetails} object is returned </li>
 * </ul>
 *
 * @author Kirill Likhodedov
 */
abstract class AbstractDataGetter<T extends VcsShortCommitDetails> implements Disposable, DataGetter<T> {
  private static final Logger LOG = Logger.getInstance(AbstractDataGetter.class);

  private static final int MAX_LOADING_TASKS = 10;

  @Nonnull
  protected final VcsLogStorage myHashMap;
  @Nonnull
  private final Map<VirtualFile, VcsLogProvider> myLogProviders;
  @Nonnull
  private final VcsCommitCache<Integer, T> myCache;
  @Nonnull
  private final SequentialLimitedLifoExecutor<TaskDescriptor> myLoader;

  /**
   * The sequence number of the current "loading" task.
   */
  private long myCurrentTaskIndex = 0;

  @Nonnull
  private final Collection<Runnable> myLoadingFinishedListeners = new ArrayList<>();
  @Nonnull
  private VcsLogIndex myIndex;

  AbstractDataGetter(@Nonnull VcsLogStorage hashMap,
                     @Nonnull Map<VirtualFile, VcsLogProvider> logProviders,
                     @Nonnull VcsCommitCache<Integer, T> cache,
                     @Nonnull VcsLogIndex index,
                     @Nonnull Disposable parentDisposable) {
    myHashMap = hashMap;
    myLogProviders = logProviders;
    myCache = cache;
    myIndex = index;
    Disposer.register(parentDisposable, this);
    myLoader = new SequentialLimitedLifoExecutor<>(this, MAX_LOADING_TASKS, task -> {
      preLoadCommitData(task.myCommits);
      notifyLoaded();
    });
  }

  private void notifyLoaded() {
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
      for (Runnable loadingFinishedListener : myLoadingFinishedListeners) {
        loadingFinishedListener.run();
      }
    });
  }

  @Override
  public void dispose() {
    myLoadingFinishedListeners.clear();
  }

  @Override
  @Nonnull
  public T getCommitData(@Nonnull Integer hash, @Nonnull Iterable<Integer> neighbourHashes) {
    assert EventQueue.isDispatchThread();
    T details = getFromCache(hash);
    if (details != null) {
      return details;
    }

    runLoadCommitsData(neighbourHashes);

    T result = myCache.get(hash);
    assert result != null; // now it is in the cache as "Loading Details" (runLoadCommitsData puts it there)
    return result;
  }

  @Override
  public void loadCommitsData(@Nonnull List<Integer> hashes, @Nonnull Consumer<List<T>> consumer, @Nullable ProgressIndicator indicator) {
    assert EventQueue.isDispatchThread();
    loadCommitsData(getCommitsMap(hashes), consumer, indicator);
  }

  private void loadCommitsData(@Nonnull final TIntIntHashMap commits, @Nonnull final Consumer<List<T>> consumer, @Nullable ProgressIndicator indicator) {
    final List<T> result = ContainerUtil.newArrayList();
    final TIntHashSet toLoad = new TIntHashSet();

    long taskNumber = myCurrentTaskIndex++;

    for (int id : commits.keys()) {
      T details = getFromCache(id);
      if (details == null || details instanceof LoadingDetails) {
        toLoad.add(id);
        cacheCommit(id, taskNumber);
      }
      else {
        result.add(details);
      }
    }

    if (toLoad.isEmpty()) {
      sortCommitsByRow(result, commits);
      consumer.consume(result);
    }
    else {
      Task.Backgroundable task = new Task.Backgroundable(null, "Loading Selected Details", true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
        @Override
        public void run(@Nonnull final ProgressIndicator indicator) {
          indicator.checkCanceled();
          try {
            TIntObjectHashMap<T> map = preLoadCommitData(toLoad);
            map.forEachValue(value -> {
              result.add(value);
              return true;
            });
            sortCommitsByRow(result, commits);
            notifyLoaded();
          }
          catch (VcsException e) {
            LOG.error(e);
          }
        }

        @Override
        public void onSuccess() {
          consumer.consume(result);
        }
      };
      if (indicator != null) {
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, indicator);
      }
      else {
        ProgressManager.getInstance().run(task);
      }
    }
  }

  private void sortCommitsByRow(@Nonnull List<T> result, @Nonnull final TIntIntHashMap rowsForCommits) {
    ContainerUtil.sort(result, (details1, details2) -> {
      int row1 = rowsForCommits.get(myHashMap.getCommitIndex(details1.getId(), details1.getRoot()));
      int row2 = rowsForCommits.get(myHashMap.getCommitIndex(details2.getId(), details2.getRoot()));
      return Comparing.compare(row1, row2);
    });
  }

  @Override
  @Nullable
  public T getCommitDataIfAvailable(int hash) {
    return getFromCache(hash);
  }

  @Nullable
  private T getFromCache(@Nonnull Integer commitId) {
    T details = myCache.get(commitId);
    if (details != null) {
      if (details instanceof LoadingDetails) {
        if (((LoadingDetails)details).getLoadingTaskIndex() <= myCurrentTaskIndex - MAX_LOADING_TASKS) {
          // don't let old "loading" requests stay in the cache forever
          myCache.remove(commitId);
          return null;
        }
      }
      return details;
    }
    return getFromAdditionalCache(commitId);
  }

  /**
   * Lookup somewhere else but the standard cache.
   */
  @Nullable
  protected abstract T getFromAdditionalCache(int commitId);

  private void runLoadCommitsData(@Nonnull Iterable<Integer> hashes) {
    long taskNumber = myCurrentTaskIndex++;
    TIntIntHashMap commits = getCommitsMap(hashes);
    TIntHashSet toLoad = new TIntHashSet();

    for (int id : commits.keys()) {
      cacheCommit(id, taskNumber);
      toLoad.add(id);
    }

    myLoader.queue(new TaskDescriptor(toLoad));
  }

  private void cacheCommit(final int commitId, long taskNumber) {
    // fill the cache with temporary "Loading" values to avoid producing queries for each commit that has not been cached yet,
    // even if it will be loaded within a previous query
    if (!myCache.isKeyCached(commitId)) {
      myCache.put(commitId, (T)new IndexedDetails(myIndex, myHashMap, commitId, taskNumber));
    }
  }

  @Nonnull
  private static TIntIntHashMap getCommitsMap(@Nonnull Iterable<Integer> hashes) {
    TIntIntHashMap commits = new TIntIntHashMap();
    int row = 0;
    for (Integer commitId : hashes) {
      commits.put(commitId, row);
      row++;
    }
    return commits;
  }

  @Nonnull
  public TIntObjectHashMap<T> preLoadCommitData(@Nonnull TIntHashSet commits) throws VcsException {
    TIntObjectHashMap<T> result = new TIntObjectHashMap<>();
    final MultiMap<VirtualFile, String> rootsAndHashes = MultiMap.create();
    commits.forEach(commit -> {
      CommitId commitId = myHashMap.getCommitId(commit);
      if (commitId != null) {
        rootsAndHashes.putValue(commitId.getRoot(), commitId.getHash().asString());
      }
      return true;
    });

    for (Map.Entry<VirtualFile, Collection<String>> entry : rootsAndHashes.entrySet()) {
      VcsLogProvider logProvider = myLogProviders.get(entry.getKey());
      if (logProvider != null) {
        List<? extends T> details = readDetails(logProvider, entry.getKey(), ContainerUtil.newArrayList(entry.getValue()));
        for (T data : details) {
          int index = myHashMap.getCommitIndex(data.getId(), data.getRoot());
          result.put(index, data);
        }
        saveInCache(result);
      }
      else {
        LOG.error("No log provider for root " + entry.getKey().getPath() + ". All known log providers " + myLogProviders);
      }
    }

    return result;
  }

  public void saveInCache(@Nonnull TIntObjectHashMap<T> details) {
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> details.forEachEntry((key, value) -> {
      myCache.put(key, value);
      return true;
    }));
  }

  @Nonnull
  protected abstract List<? extends T> readDetails(@Nonnull VcsLogProvider logProvider, @Nonnull VirtualFile root, @Nonnull List<String> hashes)
          throws VcsException;

  /**
   * This listener will be notified when any details loading process finishes.
   * The notification will happen in the EDT.
   */
  public void addDetailsLoadedListener(@Nonnull Runnable runnable) {
    myLoadingFinishedListeners.add(runnable);
  }

  public void removeDetailsLoadedListener(@Nonnull Runnable runnable) {
    myLoadingFinishedListeners.remove(runnable);
  }

  private static class TaskDescriptor {
    @Nonnull
    private final TIntHashSet myCommits;

    private TaskDescriptor(@Nonnull TIntHashSet commits) {
      myCommits = commits;
    }
  }
}
