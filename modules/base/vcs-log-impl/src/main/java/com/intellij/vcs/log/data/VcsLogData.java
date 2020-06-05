/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.vcs.log.data;

import com.intellij.ide.caches.CachesInvalidator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import com.intellij.openapi.progress.BackgroundTaskQueue;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import consulo.disposer.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.ThrowableConsumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.data.index.VcsLogIndex;
import com.intellij.vcs.log.data.index.VcsLogPersistentIndex;
import com.intellij.vcs.log.impl.FatalErrorHandler;
import com.intellij.vcs.log.impl.VcsLogCachesInvalidator;
import com.intellij.vcs.log.util.PersistentUtil;
import com.intellij.vcs.log.util.StopWatch;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VcsLogData implements Disposable, VcsLogDataProvider {
  private static final Logger LOG = Logger.getInstance(VcsLogData.class);
  private static final Consumer<Exception> FAILING_EXCEPTION_HANDLER = e -> {
    if (!(e instanceof ProcessCanceledException)) {
      LOG.error(e);
    }
  };
  public static final int RECENT_COMMITS_COUNT = Registry.intValue("vcs.log.recent.commits.count", 1000);

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final Map<VirtualFile, VcsLogProvider> myLogProviders;
  @Nonnull
  private final BackgroundTaskQueue myDataLoaderQueue;
  @Nonnull
  private final MiniDetailsGetter myMiniDetailsGetter;
  @Nonnull
  private final CommitDetailsGetter myDetailsGetter;

  /**
   * Current user name, as specified in the VCS settings.
   * It can be configured differently for different roots => store in a map.
   */
  private final Map<VirtualFile, VcsUser> myCurrentUser = ContainerUtil.newHashMap();

  /**
   * Cached details of the latest commits.
   * We store them separately from the cache of {@link DataGetter}, to make sure that they are always available,
   * which is important because these details will be constantly visible to the user,
   * thus it would be annoying to re-load them from VCS if the cache overflows.
   */
  @Nonnull
  private final TopCommitsCache myTopCommitsDetailsCache;
  @Nonnull
  private final VcsUserRegistryImpl myUserRegistry;
  @Nonnull
  private final VcsLogStorage myHashMap;
  @Nonnull
  private final ContainingBranchesGetter myContainingBranchesGetter;
  @Nonnull
  private final VcsLogRefresherImpl myRefresher;
  @Nonnull
  private final List<DataPackChangeListener> myDataPackChangeListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  @Nonnull
  private final FatalErrorHandler myFatalErrorsConsumer;
  @Nonnull
  private final VcsLogIndex myIndex;

  public VcsLogData(@Nonnull Project project,
                    @Nonnull Map<VirtualFile, VcsLogProvider> logProviders,
                    @Nonnull FatalErrorHandler fatalErrorsConsumer) {
    myProject = project;
    myLogProviders = logProviders;
    myDataLoaderQueue = new BackgroundTaskQueue(project, "Loading history...");
    myUserRegistry = (VcsUserRegistryImpl)ServiceManager.getService(project, VcsUserRegistry.class);
    myFatalErrorsConsumer = fatalErrorsConsumer;

    VcsLogProgress progress = new VcsLogProgress();
    Disposer.register(this, progress);

    VcsLogCachesInvalidator invalidator = CachesInvalidator.EP_NAME.findExtension(VcsLogCachesInvalidator.class);
    if (invalidator.isValid()) {
      myHashMap = createLogHashMap();
      myIndex = new VcsLogPersistentIndex(myProject, myHashMap, progress, logProviders, myFatalErrorsConsumer, this);
    }
    else {
      // this is not recoverable
      // restart won't help here
      // and can not shut down ide because of this
      // so use memory storage (probably leading to out of memory at some point) + no index
      String message = "Could not delete " + PersistentUtil.LOG_CACHE + "\nDelete it manually and restart IDEA.";
      LOG.error(message);
      myFatalErrorsConsumer.displayFatalErrorMessage(message);
      myHashMap = new InMemoryStorage();
      myIndex = new EmptyIndex();
    }

    myTopCommitsDetailsCache = new TopCommitsCache(myHashMap);
    myMiniDetailsGetter = new MiniDetailsGetter(myHashMap, logProviders, myTopCommitsDetailsCache, myIndex, this);
    myDetailsGetter = new CommitDetailsGetter(myHashMap, logProviders, myIndex, this);

    myRefresher = new VcsLogRefresherImpl(myProject, myHashMap, myLogProviders, myUserRegistry, myIndex, progress, myTopCommitsDetailsCache,
                                          this::fireDataPackChangeEvent, FAILING_EXCEPTION_HANDLER, RECENT_COMMITS_COUNT);

    myContainingBranchesGetter = new ContainingBranchesGetter(this, this);
  }

  @Nonnull
  private VcsLogStorage createLogHashMap() {
    VcsLogStorage hashMap;
    try {
      hashMap = new VcsLogStorageImpl(myProject, myLogProviders, myFatalErrorsConsumer, this);
    }
    catch (IOException e) {
      hashMap = new InMemoryStorage();
      LOG.error("Falling back to in-memory hashes", e);
    }
    return hashMap;
  }

  private void fireDataPackChangeEvent(@Nonnull final DataPack dataPack) {
    ApplicationManager.getApplication().invokeLater(() -> {
      for (DataPackChangeListener listener : myDataPackChangeListeners) {
        listener.onDataPackChange(dataPack);
      }
    });
  }

  public void addDataPackChangeListener(@Nonnull final DataPackChangeListener listener) {
    myDataPackChangeListeners.add(listener);
  }

  public void removeDataPackChangeListener(@Nonnull DataPackChangeListener listener) {
    myDataPackChangeListeners.remove(listener);
  }

  @Nonnull
  public DataPack getDataPack() {
    return myRefresher.getCurrentDataPack();
  }

  @Nonnull
  public VisiblePackBuilder createVisiblePackBuilder() {
    return new VisiblePackBuilder(myLogProviders, myHashMap, myTopCommitsDetailsCache, myDetailsGetter, myIndex);
  }

  @Override
  @Nullable
  public CommitId getCommitId(int commitIndex) {
    return myHashMap.getCommitId(commitIndex);
  }

  @Override
  public int getCommitIndex(@Nonnull Hash hash, @Nonnull VirtualFile root) {
    return myHashMap.getCommitIndex(hash, root);
  }

  @Nonnull
  public VcsLogStorage getHashMap() {
    return myHashMap;
  }

  public void initialize() {
    final StopWatch initSw = StopWatch.start("initialize");
    myDataLoaderQueue.clear();

    runInBackground(indicator -> {
      resetState();
      readCurrentUser();
      DataPack dataPack = myRefresher.readFirstBlock();
      fireDataPackChangeEvent(dataPack);
      initSw.report();
    });
  }

  private void readCurrentUser() {
    StopWatch sw = StopWatch.start("readCurrentUser");
    for (Map.Entry<VirtualFile, VcsLogProvider> entry : myLogProviders.entrySet()) {
      VirtualFile root = entry.getKey();
      try {
        VcsUser me = entry.getValue().getCurrentUser(root);
        if (me != null) {
          myCurrentUser.put(root, me);
        }
        else {
          LOG.info("Username not configured for root " + root);
        }
      }
      catch (VcsException e) {
        LOG.warn("Couldn't read the username from root " + root, e);
      }
    }
    sw.report();
  }

  private void resetState() {
    myTopCommitsDetailsCache.clear();
  }

  @Nonnull
  public Set<VcsUser> getAllUsers() {
    return myUserRegistry.getUsers();
  }

  @Nonnull
  public Map<VirtualFile, VcsUser> getCurrentUser() {
    return myCurrentUser;
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  public Collection<VirtualFile> getRoots() {
    return myLogProviders.keySet();
  }

  @Nonnull
  public Map<VirtualFile, VcsLogProvider> getLogProviders() {
    return myLogProviders;
  }

  @Nonnull
  public ContainingBranchesGetter getContainingBranchesGetter() {
    return myContainingBranchesGetter;
  }

  private void runInBackground(@Nonnull ThrowableConsumer<ProgressIndicator, VcsException> task) {
    Task.Backgroundable backgroundable = new Task.Backgroundable(myProject, "Loading History...", false) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        try {
          task.consume(indicator);
        }
        catch (VcsException e) {
          throw new RuntimeException(e); // TODO
        }
      }
    };
    myDataLoaderQueue.run(backgroundable, null, myRefresher.getProgress().createProgressIndicator());
  }

  /**
   * Refreshes all the roots.
   * Does not re-read all log but rather the most recent commits.
   */
  public void refreshSoftly() {
    myRefresher.refresh(myLogProviders.keySet());
  }

  /**
   * Makes the log perform refresh for the given root.
   * This refresh can be optimized, i. e. it can query VCS just for the part of the log.
   */
  public void refresh(@Nonnull Collection<VirtualFile> roots) {
    myRefresher.refresh(roots);
  }

  public CommitDetailsGetter getCommitDetailsGetter() {
    return myDetailsGetter;
  }

  @Nonnull
  public MiniDetailsGetter getMiniDetailsGetter() {
    return myMiniDetailsGetter;
  }

  @Override
  public void dispose() {
    myDataLoaderQueue.clear();
    resetState();
  }

  @Nonnull
  public VcsLogProvider getLogProvider(@Nonnull VirtualFile root) {
    return myLogProviders.get(root);
  }

  @Nonnull
  public VcsUserRegistryImpl getUserRegistry() {
    return myUserRegistry;
  }

  @Nonnull
  public VcsLogProgress getProgress() {
    return myRefresher.getProgress();
  }

  @Nonnull
  public TopCommitsCache getTopCommitsCache() {
    return myTopCommitsDetailsCache;
  }

  @Nonnull
  public VcsLogIndex getIndex() {
    return myIndex;
  }
}
