/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.graph.GraphCommit;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.impl.internal.RequirementsImpl;
import consulo.versionControlSystem.log.impl.internal.data.index.VcsLogIndex;
import consulo.versionControlSystem.log.impl.internal.graph.GraphCommitImpl;
import consulo.versionControlSystem.util.StopWatch;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VcsLogRefresherImpl implements VcsLogRefresher {

  private static final Logger LOG = Logger.getInstance(VcsLogRefresherImpl.class);

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final VcsLogStorage myHashMap;
  @Nonnull
  private final Map<VirtualFile, VcsLogProvider> myProviders;
  @Nonnull
  private final VcsUserRegistryImpl myUserRegistry;
  @Nonnull
  private final VcsLogIndex myIndex;
  @Nonnull
  private final TopCommitsCache myTopCommitsDetailsCache;
  @Nonnull
  private final Consumer<Exception> myExceptionHandler;
  @Nonnull
  private final VcsLogProgress myProgress;

  private final int myRecentCommitCount;

  @Nonnull
  private final SingleTaskController<RefreshRequest, DataPack> mySingleTaskController;

  @Nonnull
  private volatile DataPack myDataPack = DataPack.EMPTY;

  public VcsLogRefresherImpl(@Nonnull Project project,
                             @Nonnull VcsLogStorage hashMap,
                             @Nonnull Map<VirtualFile, VcsLogProvider> providers,
                             @Nonnull VcsUserRegistryImpl userRegistry,
                             @Nonnull VcsLogIndex index,
                             @Nonnull VcsLogProgress progress,
                             @Nonnull TopCommitsCache topCommitsDetailsCache,
                             @Nonnull Consumer<DataPack> dataPackUpdateHandler,
                             @Nonnull Consumer<Exception> exceptionHandler,
                             int recentCommitsCount) {
    myProject = project;
    myHashMap = hashMap;
    myProviders = providers;
    myUserRegistry = userRegistry;
    myIndex = index;
    myTopCommitsDetailsCache = topCommitsDetailsCache;
    myExceptionHandler = exceptionHandler;
    myRecentCommitCount = recentCommitsCount;
    myProgress = progress;

    mySingleTaskController = new SingleTaskController<>(dataPack -> {
      myDataPack = dataPack;
      dataPackUpdateHandler.accept(dataPack);
    }) {
      @Override
      protected void startNewBackgroundTask() {
        VcsLogRefresherImpl.this.startNewBackgroundTask(new MyRefreshTask(myDataPack));
      }
    };
  }

  protected void startNewBackgroundTask(@Nonnull final Task.Backgroundable refreshTask) {
    UIUtil.invokeLaterIfNeeded(() -> {
      LOG.debug("Starting a background task...");
      ProgressManager.getInstance().runProcessWithProgressAsynchronously(refreshTask, myProgress.createProgressIndicator());
    });
  }

  @Nonnull
  public DataPack getCurrentDataPack() {
    return myDataPack;
  }

  @Nonnull
  @Override
  public DataPack readFirstBlock() {
    try {
      LogInfo data = loadRecentData(new CommitCountRequirements(myRecentCommitCount).asMap(myProviders.keySet()));
      Collection<List<GraphCommit<Integer>>> commits = data.getCommits();
      Map<VirtualFile, CompressedRefs> refs = data.getRefs();
      List<GraphCommit<Integer>> compoundList = multiRepoJoin(commits);
      compoundList = compoundList.subList(0, Math.min(myRecentCommitCount, compoundList.size()));
      myDataPack = DataPack.build(compoundList, refs, myProviders, myHashMap, false);
      mySingleTaskController.request(RefreshRequest.RELOAD_ALL); // build/rebuild the full log in background
      return myDataPack;
    }
    catch (VcsException e) {
      myExceptionHandler.accept(e);
      return DataPack.EMPTY;
    }
  }

  @Nonnull
  private LogInfo loadRecentData(@Nonnull final Map<VirtualFile, VcsLogProvider.Requirements> requirements) throws VcsException {
    final StopWatch sw = StopWatch.start("loading commits");
    final LogInfo logInfo = new LogInfo(myHashMap);
    new ProviderIterator() {
      @Override
      public void each(@Nonnull VirtualFile root, @Nonnull VcsLogProvider provider) throws VcsException {
        VcsLogProvider.DetailedLogData data = provider.readFirstBlock(root, requirements.get(root));
        logInfo.put(root, compactCommits(data.getCommits(), root));
        logInfo.put(root, data.getRefs());
        storeUsersAndDetails(data.getCommits());
        sw.rootCompleted(root);
      }
    }.iterate(getProvidersForRoots(requirements.keySet()));
    myUserRegistry.flush();
    myIndex.scheduleIndex(false);
    sw.report();
    return logInfo;
  }

  @Nonnull
  private Map<VirtualFile, VcsLogProvider> getProvidersForRoots(@Nonnull Set<VirtualFile> roots) {
    return ContainerUtil.map2Map(roots, root -> Pair.create(root, myProviders.get(root)));
  }

  @Override
  public void refresh(@Nonnull Collection<VirtualFile> rootsToRefresh) {
    if (!rootsToRefresh.isEmpty()) {
      mySingleTaskController.request(new RefreshRequest(rootsToRefresh));
    }
  }

  @Nonnull
  private static <T extends GraphCommit<Integer>> List<T> multiRepoJoin(@Nonnull Collection<List<T>> commits) {
    StopWatch sw = StopWatch.start("multi-repo join");
    List<T> joined = new VcsLogMultiRepoJoiner<Integer, T>().join(commits);
    sw.report();
    return joined;
  }

  @Nonnull
  private List<GraphCommit<Integer>> compactCommits(@Nonnull List<? extends TimedVcsCommit> commits, @Nonnull final VirtualFile root) {
    StopWatch sw = StopWatch.start("compacting commits");
    List<GraphCommit<Integer>> map = ContainerUtil.map(commits, new Function<TimedVcsCommit, GraphCommit<Integer>>() {
      @Nonnull
      @Override
      public GraphCommit<Integer> apply(@Nonnull TimedVcsCommit commit) {
        return compactCommit(commit, root);
      }
    });
    myHashMap.flush();
    sw.report();
    return map;
  }

  @Nonnull
  private GraphCommitImpl<Integer> compactCommit(@Nonnull TimedVcsCommit commit, @Nonnull final VirtualFile root) {
    List<Integer> parents = ContainerUtil.map(commit.getParents(), hash -> myHashMap.getCommitIndex(hash, root));
    int index = myHashMap.getCommitIndex(commit.getId(), root);
    myIndex.markForIndexing(index, root);
    return new GraphCommitImpl<>(index, parents, commit.getTimestamp());
  }

  private void storeUsersAndDetails(@Nonnull List<? extends VcsCommitMetadata> metadatas) {
    for (VcsCommitMetadata detail : metadatas) {
      myUserRegistry.addUser(detail.getAuthor());
      myUserRegistry.addUser(detail.getCommitter());
    }
    myTopCommitsDetailsCache.storeDetails(metadatas);
  }

  @Nonnull
  public VcsLogProgress getProgress() {
    return myProgress;
  }

  private class MyRefreshTask extends Task.Backgroundable {

    @Nonnull
    private DataPack myCurrentDataPack;
    @Nonnull
    private final LogInfo myLoadedInfo = new LogInfo(myHashMap);

    MyRefreshTask(@Nonnull DataPack currentDataPack) {
      super(VcsLogRefresherImpl.this.myProject, "Refreshing History...", false);
      myCurrentDataPack = currentDataPack;
    }

    @Override
    public void run(@Nonnull ProgressIndicator indicator) {
      LOG.debug("Refresh task started");
      indicator.setIndeterminate(true);
      DataPack dataPack = myCurrentDataPack;
      while (true) {
        List<RefreshRequest> requests = mySingleTaskController.popRequests();
        Collection<VirtualFile> rootsToRefresh = getRootsToRefresh(requests);
        LOG.debug("Requests: " + requests + ". roots to refresh: " + rootsToRefresh);
        if (rootsToRefresh.isEmpty()) {
          mySingleTaskController.taskCompleted(dataPack);
          break;
        }
        dataPack = doRefresh(rootsToRefresh);
      }
    }

    @Nonnull
    private Collection<VirtualFile> getRootsToRefresh(@Nonnull List<RefreshRequest> requests) {
      Collection<VirtualFile> rootsToRefresh = new ArrayList<>();
      for (RefreshRequest request : requests) {
        if (request == RefreshRequest.RELOAD_ALL) {
          myCurrentDataPack = DataPack.EMPTY;
          return myProviders.keySet();
        }
        rootsToRefresh.addAll(request.rootsToRefresh);
      }
      return rootsToRefresh;
    }

    @Nonnull
    private DataPack doRefresh(@Nonnull Collection<VirtualFile> roots) {
      StopWatch sw = StopWatch.start("refresh");
      PermanentGraph<Integer> permanentGraph = myCurrentDataPack.isFull() ? myCurrentDataPack.getPermanentGraph() : null;
      Map<VirtualFile, CompressedRefs> currentRefs = myCurrentDataPack.getRefsModel().getAllRefsByRoot();
      try {
        if (permanentGraph != null) {
          int commitCount = myRecentCommitCount;
          for (int attempt = 0; attempt <= 1; attempt++) {
            loadLogAndRefs(roots, currentRefs, commitCount);
            List<? extends GraphCommit<Integer>> compoundLog = multiRepoJoin(myLoadedInfo.getCommits());
            Map<VirtualFile, CompressedRefs> allNewRefs = getAllNewRefs(myLoadedInfo, currentRefs);
            List<GraphCommit<Integer>> joinedFullLog = join(compoundLog, permanentGraph.getAllCommits(), currentRefs, allNewRefs);
            if (joinedFullLog == null) {
              commitCount *= 5;
            }
            else {
              return DataPack.build(joinedFullLog, allNewRefs, myProviders, myHashMap, true);
            }
          }
          // couldn't join => need to reload everything; if 5000 commits is still not enough, it's worth reporting:
          LOG.info("Couldn't join " + commitCount / 5 + " recent commits to the log (" +
                   permanentGraph.getAllCommits().size() + " commits)");
        }

        return loadFullLog();
      }
      catch (Exception e) {
        myExceptionHandler.accept(e);
        return DataPack.EMPTY;
      }
      finally {
        sw.report();
      }
    }

    @Nonnull
    private Map<VirtualFile, CompressedRefs> getAllNewRefs(
      @Nonnull LogInfo newInfo,
      @Nonnull Map<VirtualFile, CompressedRefs> previousRefs
    ) {
      Map<VirtualFile, CompressedRefs> result = new HashMap<>();
      for (VirtualFile root : previousRefs.keySet()) {
        CompressedRefs newInfoRefs = newInfo.getRefs().get(root);
        result.put(root, newInfoRefs != null ? newInfoRefs : previousRefs.get(root));
      }
      return result;
    }

    private void loadLogAndRefs(
      @Nonnull Collection<VirtualFile> roots,
      @Nonnull Map<VirtualFile, CompressedRefs> prevRefs,
      int commitCount
    ) throws VcsException {
      LogInfo logInfo = loadRecentData(prepareRequirements(roots, commitCount, prevRefs));
      for (VirtualFile root : roots) {
        myLoadedInfo.put(root, logInfo.getCommits(root));
        myLoadedInfo.put(root, logInfo.getRefs().get(root));
      }
    }

    @Nonnull
    private Map<VirtualFile, VcsLogProvider.Requirements> prepareRequirements(
      @Nonnull Collection<VirtualFile> roots,
      int commitCount,
      @Nonnull Map<VirtualFile, CompressedRefs> prevRefs
    ) {
      Map<VirtualFile, VcsLogProvider.Requirements> requirements = new HashMap<>();
      for (VirtualFile root : roots) {
        requirements.put(root, new RequirementsImpl(commitCount, true, prevRefs.get(root).getRefs()));
      }
      return requirements;
    }

    @Nullable
    private List<GraphCommit<Integer>> join(
      @Nonnull List<? extends GraphCommit<Integer>> recentCommits,
      @Nonnull List<GraphCommit<Integer>> fullLog,
      @Nonnull Map<VirtualFile, CompressedRefs> previousRefs,
      @Nonnull Map<VirtualFile, CompressedRefs> newRefs
    ) {
      StopWatch sw = StopWatch.start("joining new commits");
      Collection<Integer> prevRefIndices =
              previousRefs.values().stream().flatMap(refs -> refs.getCommits().stream()).collect(Collectors.toSet());
      Collection<Integer> newRefIndices = newRefs.values().stream().flatMap(refs -> refs.getCommits().stream()).collect(Collectors.toSet());
      try {
        List<GraphCommit<Integer>> commits = new VcsLogJoiner<Integer, GraphCommit<Integer>>().addCommits(fullLog, prevRefIndices,
                                                                                                          recentCommits,
                                                                                                          newRefIndices).first;
        sw.report();
        return commits;
      }
      catch (VcsLogRefreshNotEnoughDataException e) {
        // valid case: e.g. another developer merged a long-developed branch, or we just didn't pull for a long time
        LOG.info(e);
      }
      catch (IllegalStateException e) {
        // it happens from time to time, but we don't know why, and can hardly debug it.
        LOG.info(e);
      }
      return null;
    }

    @Nonnull
    private DataPack loadFullLog() throws VcsException {
      StopWatch sw = StopWatch.start("full log reload");
      LogInfo logInfo = readFullLogFromVcs();
      List<? extends GraphCommit<Integer>> graphCommits = multiRepoJoin(logInfo.getCommits());
      DataPack dataPack = DataPack.build(graphCommits, logInfo.getRefs(), myProviders, myHashMap, true);
      sw.report();
      return dataPack;
    }

    @Nonnull
    private LogInfo readFullLogFromVcs() throws VcsException {
      final StopWatch sw = StopWatch.start("read full log from VCS");
      final LogInfo logInfo = new LogInfo(myHashMap);
      new ProviderIterator() {
        @Override
        void each(@Nonnull final VirtualFile root, @Nonnull VcsLogProvider provider) throws VcsException {
          final List<GraphCommit<Integer>> graphCommits = new ArrayList<>();
          VcsLogProvider.LogData data = provider.readAllHashes(root, commit -> graphCommits.add(compactCommit(commit, root)));
          logInfo.put(root, graphCommits);
          logInfo.put(root, data.getRefs());
          myUserRegistry.addUsers(data.getUsers());
          sw.rootCompleted(root);
        }
      }.iterate(myProviders);
      myUserRegistry.flush();
      myIndex.scheduleIndex(true);
      sw.report();
      return logInfo;
    }
  }

  private static class RefreshRequest {
    private static final RefreshRequest RELOAD_ALL = new RefreshRequest(Collections.<VirtualFile>emptyList()) {
      @Override
      public String toString() {
        return "RELOAD_ALL";
      }
    };
    private final Collection<VirtualFile> rootsToRefresh;

    RefreshRequest(@Nonnull Collection<VirtualFile> rootsToRefresh) {
      this.rootsToRefresh = rootsToRefresh;
    }

    @Override
    public String toString() {
      return "{" + rootsToRefresh + "}";
    }
  }

  private static abstract class ProviderIterator {
    abstract void each(@Nonnull VirtualFile root, @Nonnull VcsLogProvider provider) throws VcsException;

    final void iterate(@Nonnull Map<VirtualFile, VcsLogProvider> providers) throws VcsException {
      for (Map.Entry<VirtualFile, VcsLogProvider> entry : providers.entrySet()) {
        each(entry.getKey(), entry.getValue());
      }
    }
  }

  private static class CommitCountRequirements implements VcsLogProvider.Requirements {
    private final int myCommitCount;

    public CommitCountRequirements(int commitCount) {
      myCommitCount = commitCount;
    }

    @Override
    public int getCommitCount() {
      return myCommitCount;
    }

    @Nonnull
    Map<VirtualFile, VcsLogProvider.Requirements> asMap(@Nonnull Collection<VirtualFile> roots) {
      return ContainerUtil.map2Map(roots, root -> Pair.<VirtualFile, VcsLogProvider.Requirements>create(root, this));
    }
  }

  @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
  private static class LogInfo {
    private final VcsLogStorage myHashMap;
    private final Map<VirtualFile, CompressedRefs> myRefs = new HashMap<>();
    private final Map<VirtualFile, List<GraphCommit<Integer>>> myCommits = new HashMap<>();

    public LogInfo(VcsLogStorage hashMap) {
      myHashMap = hashMap;
    }

    void put(@Nonnull VirtualFile root, @Nonnull List<GraphCommit<Integer>> commits) {
      myCommits.put(root, commits);
    }

    void put(@Nonnull VirtualFile root, @Nonnull Set<VcsRef> refs) {
      myRefs.put(root, new CompressedRefs(refs, myHashMap));
    }

    void put(@Nonnull VirtualFile root, @Nonnull CompressedRefs refs) {
      myRefs.put(root, refs);
    }

    @Nonnull
    Collection<List<GraphCommit<Integer>>> getCommits() {
      return myCommits.values();
    }

    List<GraphCommit<Integer>> getCommits(@Nonnull VirtualFile root) {
      return myCommits.get(root);
    }

    @Nonnull
    Map<VirtualFile, CompressedRefs> getRefs() {
      return myRefs;
    }
  }
}
