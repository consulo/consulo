package consulo.versionControlSystem.log.impl.internal.data;

import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.VcsShortCommitDetails;
import consulo.versionControlSystem.log.impl.internal.data.index.IndexedDetails;
import consulo.versionControlSystem.log.impl.internal.data.index.VcsLogIndex;
import consulo.versionControlSystem.log.impl.internal.util.SequentialLimitedLifoExecutor;
import consulo.virtualFileSystem.VirtualFile;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

  
  protected final VcsLogStorage myHashMap;
  
  private final Map<VirtualFile, VcsLogProvider> myLogProviders;
  
  private final VcsCommitCache<Integer, T> myCache;
  
  private final SequentialLimitedLifoExecutor<TaskDescriptor> myLoader;

  /**
   * The sequence number of the current "loading" task.
   */
  private long myCurrentTaskIndex = 0;

  
  private final Collection<Runnable> myLoadingFinishedListeners = new ArrayList<>();
  
  private VcsLogIndex myIndex;

  AbstractDataGetter(VcsLogStorage hashMap,
                     Map<VirtualFile, VcsLogProvider> logProviders,
                     VcsCommitCache<Integer, T> cache,
                     VcsLogIndex index,
                     Disposable parentDisposable) {
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
  
  public T getCommitData(Integer hash, Iterable<Integer> neighbourHashes) {
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
  public void loadCommitsData(List<Integer> hashes, Consumer<List<T>> consumer, @Nullable ProgressIndicator indicator) {
    assert EventQueue.isDispatchThread();
    loadCommitsData(getCommitsMap(hashes), consumer, indicator);
  }                                                                                     

  private void loadCommitsData(final TIntIntHashMap commits, final Consumer<List<T>> consumer, @Nullable ProgressIndicator indicator) {
    final List<T> result = new ArrayList<T>();
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
      consumer.accept(result);
    }
    else {
      Task.Backgroundable task = new Task.Backgroundable(null, "Loading Selected Details", true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
        @Override
        public void run(ProgressIndicator indicator) {
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

        @RequiredUIAccess
        @Override
        public void onSuccess() {
          consumer.accept(result);
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

  private void sortCommitsByRow(List<T> result, TIntIntHashMap rowsForCommits) {
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
  private T getFromCache(Integer commitId) {
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

  private void runLoadCommitsData(Iterable<Integer> hashes) {
    long taskNumber = myCurrentTaskIndex++;
    TIntIntHashMap commits = getCommitsMap(hashes);
    TIntHashSet toLoad = new TIntHashSet();

    for (int id : commits.keys()) {
      cacheCommit(id, taskNumber);
      toLoad.add(id);
    }

    myLoader.queue(new TaskDescriptor(toLoad));
  }

  private void cacheCommit(int commitId, long taskNumber) {
    // fill the cache with temporary "Loading" values to avoid producing queries for each commit that has not been cached yet,
    // even if it will be loaded within a previous query
    if (!myCache.isKeyCached(commitId)) {
      myCache.put(commitId, (T)new IndexedDetails(myIndex, myHashMap, commitId, taskNumber));
    }
  }

  
  private static TIntIntHashMap getCommitsMap(Iterable<Integer> hashes) {
    TIntIntHashMap commits = new TIntIntHashMap();
    int row = 0;
    for (Integer commitId : hashes) {
      commits.put(commitId, row);
      row++;
    }
    return commits;
  }

  
  public TIntObjectHashMap<T> preLoadCommitData(TIntHashSet commits) throws VcsException {
    TIntObjectHashMap<T> result = new TIntObjectHashMap<>();
    MultiMap<VirtualFile, String> rootsAndHashes = MultiMap.create();
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

  public void saveInCache(TIntObjectHashMap<T> details) {
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> details.forEachEntry((key, value) -> {
      myCache.put(key, value);
      return true;
    }));
  }

  
  protected abstract List<? extends T> readDetails(VcsLogProvider logProvider, VirtualFile root, List<String> hashes)
          throws VcsException;

  /**
   * This listener will be notified when any details loading process finishes.
   * The notification will happen in the EDT.
   */
  public void addDetailsLoadedListener(Runnable runnable) {
    myLoadingFinishedListeners.add(runnable);
  }

  public void removeDetailsLoadedListener(Runnable runnable) {
    myLoadingFinishedListeners.remove(runnable);
  }

  private static class TaskDescriptor {
    
    private final TIntHashSet myCommits;

    private TaskDescriptor(TIntHashSet commits) {
      myCommits = commits;
    }
  }
}
