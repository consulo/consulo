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

import consulo.application.CachesInvalidator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.BackgroundTaskQueue;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.Lists;
import consulo.util.lang.function.ThrowableConsumer;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.impl.internal.FatalErrorHandler;
import consulo.versionControlSystem.log.impl.internal.VcsLogCachesInvalidator;
import consulo.versionControlSystem.log.impl.internal.data.index.VcsLogIndex;
import consulo.versionControlSystem.log.impl.internal.data.index.VcsLogPersistentIndex;
import consulo.versionControlSystem.log.impl.internal.util.PersistentUtil;
import consulo.versionControlSystem.util.StopWatch;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class VcsLogDataImpl implements VcsLogData {
    private static final Logger LOG = Logger.getInstance(VcsLogDataImpl.class);
    private static final Consumer<Exception> FAILING_EXCEPTION_HANDLER = e -> {
        if (!(e instanceof ProcessCanceledException)) {
            LOG.error(e);
        }
    };
    public static final int RECENT_COMMITS_COUNT = Registry.intValue("vcs.log.recent.commits.count", 1000);

    
    private final Project myProject;
    
    private final Map<VirtualFile, VcsLogProvider> myLogProviders;
    
    private final BackgroundTaskQueue myDataLoaderQueue;
    
    private final MiniDetailsGetter myMiniDetailsGetter;
    
    private final CommitDetailsGetter myDetailsGetter;

    /**
     * Current user name, as specified in the VCS settings.
     * It can be configured differently for different roots => store in a map.
     */
    private final Map<VirtualFile, VcsUser> myCurrentUser = new HashMap<>();

    /**
     * Cached details of the latest commits.
     * We store them separately from the cache of {@link DataGetter}, to make sure that they are always available,
     * which is important because these details will be constantly visible to the user,
     * thus it would be annoying to re-load them from VCS if the cache overflows.
     */
    private final TopCommitsCache myTopCommitsDetailsCache;
    
    private final VcsUserRegistryImpl myUserRegistry;
    
    private final VcsLogStorage myHashMap;
    
    private final ContainingBranchesGetter myContainingBranchesGetter;
    
    private final VcsLogRefresherImpl myRefresher;
    
    private final List<DataPackChangeListener> myDataPackChangeListeners = Lists.newLockFreeCopyOnWriteList();

    
    private final FatalErrorHandler myFatalErrorsConsumer;
    
    private final VcsLogIndex myIndex;

    public VcsLogDataImpl(
        Project project,
        Map<VirtualFile, VcsLogProvider> logProviders,
        FatalErrorHandler fatalErrorsConsumer
    ) {
        myProject = project;
        myLogProviders = logProviders;
        myDataLoaderQueue = new BackgroundTaskQueue(project.getApplication(), project, "Loading history...");
        myUserRegistry = (VcsUserRegistryImpl) project.getInstance(VcsUserRegistry.class);
        myFatalErrorsConsumer = fatalErrorsConsumer;

        VcsLogProgress progress = new VcsLogProgress();
        Disposer.register(this, progress);

        VcsLogCachesInvalidator invalidator = myProject.getApplication().getExtensionPoint(CachesInvalidator.class)
            .findExtensionOrFail(VcsLogCachesInvalidator.class);
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

        myRefresher = new VcsLogRefresherImpl(
            myProject,
            myHashMap,
            myLogProviders,
            myUserRegistry,
            myIndex,
            progress,
            myTopCommitsDetailsCache,
            this::fireDataPackChangeEvent,
            FAILING_EXCEPTION_HANDLER,
            RECENT_COMMITS_COUNT
        );

        myContainingBranchesGetter = new ContainingBranchesGetter(this, this);
    }

    
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

    private void fireDataPackChangeEvent(DataPack dataPack) {
        myProject.getApplication().invokeLater(() -> {
            for (DataPackChangeListener listener : myDataPackChangeListeners) {
                listener.onDataPackChange(dataPack);
            }
        });
    }

    public void addDataPackChangeListener(DataPackChangeListener listener) {
        myDataPackChangeListeners.add(listener);
    }

    public void removeDataPackChangeListener(DataPackChangeListener listener) {
        myDataPackChangeListeners.remove(listener);
    }

    
    public DataPack getDataPack() {
        return myRefresher.getCurrentDataPack();
    }

    
    public VisiblePackBuilder createVisiblePackBuilder() {
        return new VisiblePackBuilder(myLogProviders, myHashMap, myTopCommitsDetailsCache, myDetailsGetter, myIndex);
    }

    @Override
    public @Nullable CommitId getCommitId(int commitIndex) {
        return myHashMap.getCommitId(commitIndex);
    }

    @Override
    public int getCommitIndex(Hash hash, VirtualFile root) {
        return myHashMap.getCommitIndex(hash, root);
    }

    
    public VcsLogStorage getHashMap() {
        return myHashMap;
    }

    public void initialize() {
        StopWatch initSw = StopWatch.start("initialize");
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

    
    @Override
    public Set<VcsUser> getAllUsers() {
        return myUserRegistry.getUsers();
    }

    
    @Override
    public Map<VirtualFile, VcsUser> getCurrentUser() {
        return myCurrentUser;
    }

    
    @Override
    public Project getProject() {
        return myProject;
    }

    
    public Collection<VirtualFile> getRoots() {
        return myLogProviders.keySet();
    }

    
    public Map<VirtualFile, VcsLogProvider> getLogProviders() {
        return myLogProviders;
    }

    
    public ContainingBranchesGetter getContainingBranchesGetter() {
        return myContainingBranchesGetter;
    }

    private void runInBackground(ThrowableConsumer<ProgressIndicator, VcsException> task) {
        Task.Backgroundable backgroundable = new Task.Backgroundable(myProject, "Loading History...", false) {
            @Override
            public void run(ProgressIndicator indicator) {
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
    public void refresh(Collection<VirtualFile> roots) {
        myRefresher.refresh(roots);
    }

    public CommitDetailsGetter getCommitDetailsGetter() {
        return myDetailsGetter;
    }

    
    public MiniDetailsGetter getMiniDetailsGetter() {
        return myMiniDetailsGetter;
    }

    @Override
    public void dispose() {
        myDataLoaderQueue.clear();
        resetState();
    }

    
    @Override
    public VcsLogProvider getLogProvider(VirtualFile root) {
        return myLogProviders.get(root);
    }

    
    public VcsUserRegistryImpl getUserRegistry() {
        return myUserRegistry;
    }

    
    public VcsLogProgress getProgress() {
        return myRefresher.getProgress();
    }

    
    public TopCommitsCache getTopCommitsCache() {
        return myTopCommitsDetailsCache;
    }

    
    public VcsLogIndex getIndex() {
        return myIndex;
    }
}
