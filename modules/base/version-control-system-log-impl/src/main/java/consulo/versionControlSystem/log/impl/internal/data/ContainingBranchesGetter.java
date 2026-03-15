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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SLRUMap;
import consulo.util.lang.function.Predicates;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.impl.internal.util.SequentialLimitedLifoExecutor;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

/**
 * Provides capabilities to asynchronously calculate "contained in branches" information.
 */
public class ContainingBranchesGetter {

    private static final Logger LOG = Logger.getInstance(ContainingBranchesGetter.class);

    
    private final SequentialLimitedLifoExecutor<Task> myTaskExecutor;
    
    private final VcsLogDataImpl myLogData;

    // other fields accessed only from EDT
    
    private final List<Runnable> myLoadingFinishedListeners = new ArrayList<>();
    
    private SLRUMap<CommitId, List<String>> myCache = createCache();
    
    private Map<VirtualFile, ContainedInBranchCondition> myConditions = new HashMap<>();
    private int myCurrentBranchesChecksum;

    ContainingBranchesGetter(VcsLogDataImpl logData, Disposable parentDisposable) {
        myLogData = logData;
        myTaskExecutor = new SequentialLimitedLifoExecutor<>(parentDisposable, 10, task -> {
            List<String> branches = task.getContainingBranches(myLogData);
            Application.get().invokeLater(() -> {
                // if cache is cleared (because of log refresh) during this task execution,
                // this will put obsolete value into the old instance we don't care anymore
                task.cache.put(new CommitId(task.hash, task.root), branches);
                notifyListeners();
            });
        });
        myLogData.addDataPackChangeListener(dataPack -> {
            Collection<VcsRef> currentBranches = dataPack.getRefsModel().getBranches();
            int checksum = currentBranches.hashCode();
            if (myCurrentBranchesChecksum != 0 && myCurrentBranchesChecksum != checksum) { // clear cache if branches set changed after refresh
                clearCache();
            }
            myCurrentBranchesChecksum = checksum;
        });
    }

    private void clearCache() {
        myCache = createCache();
        myTaskExecutor.clear();
        Map<VirtualFile, ContainedInBranchCondition> conditions = myConditions;
        myConditions = new HashMap<>();
        for (ContainedInBranchCondition c : conditions.values()) {
            c.dispose();
        }
        // re-request containing branches information for the commit user (possibly) currently stays on
        Application.get().invokeLater(this::notifyListeners);
    }

    /**
     * This task will be executed each time the calculating process completes.
     */
    public void addTaskCompletedListener(Runnable runnable) {
        LOG.assertTrue(EventQueue.isDispatchThread());
        myLoadingFinishedListeners.add(runnable);
    }

    public void removeTaskCompletedListener(Runnable runnable) {
        LOG.assertTrue(EventQueue.isDispatchThread());
        myLoadingFinishedListeners.remove(runnable);
    }

    private void notifyListeners() {
        LOG.assertTrue(EventQueue.isDispatchThread());
        for (Runnable listener : myLoadingFinishedListeners) {
            listener.run();
        }
    }

    /**
     * Returns the alphabetically sorted list of branches containing the specified node, if this information is ready;
     * if it is not available, starts calculating in the background and returns null.
     */
    @Nullable
    public List<String> requestContainingBranches(VirtualFile root, Hash hash) {
        LOG.assertTrue(EventQueue.isDispatchThread());
        List<String> refs = myCache.get(new CommitId(hash, root));
        if (refs == null) {
            DataPack dataPack = myLogData.getDataPack();
            myTaskExecutor.queue(new Task(root, hash, myCache, dataPack.getPermanentGraph(), dataPack.getRefsModel()));
        }
        return refs;
    }

    @Nullable
    public List<String> getContainingBranchesFromCache(VirtualFile root, Hash hash) {
        synchronized (myCache) {
            return myCache.get(new CommitId(hash, root));
        }
    }

    
    public Predicate<CommitId> getContainedInBranchCondition(String branchName, VirtualFile root) {
        LOG.assertTrue(EventQueue.isDispatchThread());

        DataPack dataPack = myLogData.getDataPack();
        if (dataPack == DataPack.EMPTY) {
            return Predicates.alwaysFalse();
        }

        PermanentGraph<Integer> graph = dataPack.getPermanentGraph();
        VcsLogRefs refs = dataPack.getRefsModel();

        ContainedInBranchCondition condition = myConditions.get(root);
        if (condition == null || !condition.getBranch().equals(branchName)) {
            VcsRef branchRef =
                ContainerUtil.find(refs.getBranches(), vcsRef -> vcsRef.getRoot().equals(root) && vcsRef.getName().equals(branchName));
            if (branchRef == null) {
                return Predicates.alwaysFalse();
            }
            condition = new ContainedInBranchCondition(
                graph.getContainedInBranchCondition(Collections.singleton(myLogData.getCommitIndex(
                    branchRef.getCommitHash(),
                    branchRef.getRoot()
                ))),
                branchName
            );
            myConditions.put(root, condition);
        }
        return condition;
    }

    
    private static SLRUMap<CommitId, List<String>> createCache() {
        return new SLRUMap<>(1000, 1000);
    }

    
    public List<String> getContainingBranchesSynchronously(VirtualFile root, Hash hash) {
        return doGetContainingBranches(myLogData.getDataPack(), root, hash);
    }

    
    private List<String> doGetContainingBranches(DataPack dataPack, VirtualFile root, Hash hash) {
        return new Task(root, hash, myCache, dataPack.getPermanentGraph(), dataPack.getRefsModel()).getContainingBranches(myLogData);
    }

    private static class Task {
        private final VirtualFile root;
        private final Hash hash;
        private final SLRUMap<CommitId, List<String>> cache;
        @Nullable
        private final RefsModel refs;
        @Nullable
        private final PermanentGraph<Integer> graph;

        public Task(
            VirtualFile root,
            Hash hash,
            SLRUMap<CommitId, List<String>> cache,
            @Nullable PermanentGraph<Integer> graph,
            @Nullable RefsModel refs
        ) {
            this.root = root;
            this.hash = hash;
            this.cache = cache;
            this.graph = graph;
            this.refs = refs;
        }

        
        public List<String> getContainingBranches(VcsLogDataImpl logData) {
            try {
                VcsLogProvider provider = logData.getLogProvider(root);
                if (graph != null && refs != null && VcsLogProperties.get(provider, VcsLogProperties.LIGHTWEIGHT_BRANCHES)) {
                    Set<Integer> branchesIndexes = graph.getContainingBranches(logData.getCommitIndex(hash, root));

                    Collection<VcsRef> branchesRefs = new HashSet<>();
                    for (Integer index : branchesIndexes) {
                        refs.refsToCommit(index).stream().filter(ref -> ref.getType().isBranch()).forEach(branchesRefs::add);
                    }
                    branchesRefs = ContainerUtil.sorted(branchesRefs, provider.getReferenceManager().getLabelsOrderComparator());

                    ArrayList<String> branchesList = new ArrayList<>();
                    for (VcsRef ref : branchesRefs) {
                        branchesList.add(ref.getName());
                    }
                    return branchesList;
                }
                else {
                    List<String> branches = new ArrayList<>(provider.getContainingBranches(root, hash));
                    Collections.sort(branches);
                    return branches;
                }
            }
            catch (VcsException e) {
                LOG.warn(e);
                return Collections.emptyList();
            }
        }
    }

    private class ContainedInBranchCondition implements Predicate<CommitId> {
        
        private final Predicate<Integer> myCondition;
        
        private final String myBranch;
        private volatile boolean isDisposed = false;

        public ContainedInBranchCondition(Predicate<Integer> condition, String branch) {
            myCondition = condition;
            myBranch = branch;
        }

        
        public String getBranch() {
            return myBranch;
        }

        @Override
        public boolean test(CommitId commitId) {
            return !isDisposed && myCondition.test(myLogData.getCommitIndex(commitId.getHash(), commitId.getRoot()));
        }

        public void dispose() {
            isDisposed = true;
        }
    }
}
