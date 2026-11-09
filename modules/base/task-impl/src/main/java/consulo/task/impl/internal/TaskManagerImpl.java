/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.task.impl.internal;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountListener;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountType;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.project.ui.notification.NotificationService;
import consulo.proxy.EventDispatcher;
import consulo.task.*;
import consulo.task.event.TaskListener;
import consulo.task.impl.internal.action.TaskSearchSupport;
import consulo.task.impl.internal.context.WorkingContextManager;
import consulo.task.impl.internal.setting.TaskRepositoriesConfigurable;
import consulo.task.issue.IssueCapability;
import consulo.task.issue.IssueQuery;
import consulo.task.issue.IssueStateSupport;
import consulo.task.issue.IssueTracker;
import consulo.task.util.TaskUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsTaskHandler;
import consulo.versionControlSystem.VcsType;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author Dmitry Avdeev
 */
@Singleton
@State(name = "TaskManager", storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
@ServiceImpl
public class TaskManagerImpl extends TaskManager implements PersistentStateComponent<TaskManagerImpl.Config>, Disposable {
    private static final Logger LOG = Logger.getInstance(TaskManagerImpl.class);

    private static final DecimalFormat LOCAL_TASK_ID_FORMAT = new DecimalFormat("LOCAL-00000");
    public static final Comparator<Task> TASK_UPDATE_COMPARATOR = (o1, o2) -> {
        int i = Comparing.compare(o2.getUpdated(), o1.getUpdated());
        return i == 0 ? Comparing.compare(o2.getCreated(), o1.getCreated()) : i;
    };
    private static final Function<Task, String> KEY_CONVERTER = Task::getId;

    private final Project myProject;

    private final WorkingContextManager myContextManager;

    private final Map<String, Task> myIssueCache = Collections.synchronizedMap(new LinkedHashMap<String, Task>());

    private final Map<String, LocalTask> myTasks = Collections.synchronizedMap(new LinkedHashMap<String, LocalTask>() {
        @Override
        public LocalTask put(String key, LocalTask task) {
            LocalTask result = super.put(key, task);
            if (size() > myConfig.taskHistoryLength) {
                ArrayList<Map.Entry<String, LocalTask>> list = new ArrayList<>(entrySet());
                Collections.sort(list, (o0, o1) -> TASK_UPDATE_COMPARATOR.compare(o1.getValue(), o0.getValue()));
                for (Map.Entry<String, LocalTask> oldest : list) {
                    if (!oldest.getValue().isDefault()) {
                        remove(oldest.getKey());
                        break;
                    }
                }
            }
            return result;
        }
    });

    private LocalTask myActiveTask = createDefaultTask();
    private Future<?> myCacheRefreshTimer;

    private volatile boolean myUpdating;
    private final Config myConfig = new Config();
    private final ChangeListListener myChangeListListener;

    private final ChangeListManager myChangeListManager;

    private final NotificationService myNotificationService;

    private final AccountManager myAccountManager;
    private final TaskRepositoryStore myStore = new TaskRepositoryStore();
    private volatile List<TaskRepositoryEntry> myEntries = List.of();
    private volatile List<TaskRepository> myRepositories = List.of();
    private final EventDispatcher<TaskListener> myDispatcher = EventDispatcher.create(TaskListener.class);
    private final List<Runnable> myRepositoriesListeners = new CopyOnWriteArrayList<>();
    private Set<TaskRepository> myBadRepositories = ContainerUtil.newConcurrentSet();

    @Inject
    public TaskManagerImpl(
        Application application,
        Project project,
        WorkingContextManager contextManager,
        ChangeListManager changeListManager,
        NotificationService notificationService,
        AccountManager accountManager
    ) {
        myProject = project;
        myAccountManager = accountManager;
        myContextManager = contextManager;
        myChangeListManager = changeListManager;
        myNotificationService = notificationService;

        myChangeListListener = new ChangeListListener() {
            @Override
            public void changeListRemoved(ChangeList list) {
                LocalTask task = getAssociatedTask((LocalChangeList) list);
                if (task != null) {
                    for (ChangeListInfo info : task.getChangeLists()) {
                        if (Comparing.equal(info.id, ((LocalChangeList) list).getId())) {
                            info.id = "";
                        }
                    }
                }
            }

            @Override
            public void defaultListChanged(ChangeList oldDefaultList, ChangeList newDefaultList) {
                LocalTask associatedTask = getAssociatedTask((LocalChangeList) newDefaultList);
                if (associatedTask != null && !getActiveTask().equals(associatedTask)) {
                    application.invokeLater(() -> activateTask(associatedTask, true), myProject.getDisposed());
                }
            }
        };
    }

    @Override
    public TaskRepository[] getAllRepositories() {
        return myRepositories.toArray(new TaskRepository[0]);
    }

    @Override
    public void addRepository(TaskRepository repository) {
        List<TaskRepositoryEntry> configured = myStore.getConfigured();
        configured.add(new TaskRepositoryEntry(repository.getRepositoryType(), repository.getOptions(), false));
        setRepositoryEntries(configured, myStore.getHiddenKeys());
    }

    public List<TaskRepositoryEntry> getRepositoryEntries() {
        return myEntries;
    }

    public Set<String> getHiddenKeys() {
        return myStore.getHiddenKeys();
    }

    public void setRepositoryEntries(List<TaskRepositoryEntry> configured, Set<String> hiddenKeys) {
        myStore.set(configured, hiddenKeys);
        rebuildRepositories();
        getProjectConfiguration().getState().servers = TaskRepositoryStore.collectSharedServers(myEntries);
    }

    @Override
    public void refreshDetectedRepositories() {
        rebuildRepositories();
    }

    private void rebuildRepositories() {
        List<TaskRepositoryEntry> entries = myStore.merge(detectRepositories(), getProjectConfiguration().getState().servers, this::findRepositoryType);

        List<TaskRepository> repositories = new ArrayList<>(entries.size());
        for (TaskRepositoryEntry entry : entries) {
            try {
                repositories.add(entry.createRepository(myProject));
            }
            catch (Exception e) {
                LOG.error("Cannot create repository " + entry.getType().getId(), e);
            }
        }

        myEntries = List.copyOf(entries);
        myRepositories = List.copyOf(repositories);
        myBadRepositories.clear();
        myIssueCache.clear();

        for (Runnable listener : myRepositoriesListeners) {
            listener.run();
        }
    }

    public void addRepositoriesListener(Runnable listener, Disposable parentDisposable) {
        myRepositoriesListeners.add(listener);
        Disposer.register(parentDisposable, () -> myRepositoriesListeners.remove(listener));
    }

    private List<TaskRepositoryEntry> detectRepositories() {
        List<TaskRepositoryEntry> entries = new ArrayList<>();
        myProject.getApplication().getExtensionPoint(TaskRepositoryType.class).forEach(rawType -> {
            TaskRepositoryType<?> type = rawType;
            AccountType<?> accountType = myAccountManager.findAccountType(type.getAccountTypeId());
            if (accountType == null) {
                return;
            }
            for (Account account : myAccountManager.getAccounts(accountType, myProject)) {
                try {
                    List<? extends TaskRepositoryOptions> detected = type.detect(myProject, account);
                    for (TaskRepositoryOptions options : detected) {
                        if (options.accountId == null) {
                            options.accountId = account.getId();
                        }
                        if (options.serverUrl.isEmpty()) {
                            options.serverUrl = StringUtil.notNullize(myAccountManager.getServerUrl(account));
                        }
                        entries.add(new TaskRepositoryEntry(type, options, true));
                    }
                }
                catch (Exception e) {
                    LOG.error("Cannot detect " + type.getId() + " repositories for account " + account.getId(), e);
                }
            }
        });
        return entries;
    }

    public @Nullable TaskRepositoryType<?> findRepositoryType(String id) {
        return myProject.getApplication().getExtensionPoint(TaskRepositoryType.class).findFirstSafe(type -> type.getId().equals(id));
    }

    @Override
    public void removeTask(LocalTask task) {
        if (task.isDefault()) {
            return;
        }
        if (myActiveTask.equals(task)) {
            activateTask(myTasks.get(LocalTaskImpl.DEFAULT_TASK_ID), true);
        }
        myTasks.remove(task.getId());
        myDispatcher.getMulticaster().taskRemoved(task);
        myContextManager.removeContext(task);
    }

    @Override
    public void addTaskListener(TaskListener listener, Disposable parentDisposable) {
        myDispatcher.addListener(listener, parentDisposable);
    }

    @Override
    public LocalTask getActiveTask() {
        return myActiveTask;
    }

    @Override
    public @Nullable LocalTask findTask(String id) {
        return myTasks.get(id);
    }

    @Override
    public CompletableFuture<List<Task>> searchIssues(@Nullable String query, int offset, int limit, boolean withClosed, boolean forceRequest) {
        return searchRepositories(query, offset, limit, withClosed, forceRequest).thenApply(tasks -> {
            if (tasks == null) {
                return getCachedIssues(withClosed);
            }
            myIssueCache.putAll(ContainerUtil.newMapFromValues(tasks.iterator(), KEY_CONVERTER));
            return ContainerUtil.filter(tasks, task -> withClosed || !task.isClosed());
        });
    }

    @Override
    public List<Task> getCachedIssues() {
        return getCachedIssues(true);
    }

    @Override
    public List<Task> getCachedIssues(boolean withClosed) {
        return ContainerUtil.filter(myIssueCache.values(), task -> withClosed || !task.isClosed());
    }

    @Override
    public CompletableFuture<@Nullable Task> refreshIssue(String id) {
        CompletableFuture<@Nullable Task> result = CompletableFuture.completedFuture(null);
        for (TaskRepository repository : getAllRepositories()) {
            if (repository.extractId(id) == null) {
                continue;
            }
            result = result.thenCompose(found -> {
                if (found != null) {
                    return CompletableFuture.completedFuture(found);
                }
                return repository.getIssueTracker().findIssue(id).exceptionally(error -> {
                    LOG.warn("Cannot find task '" + id + "' in " + repository + ": " + error.getMessage());
                    return null;
                });
            });
        }
        return result.thenApply(issue -> {
            if (issue == null) {
                return null;
            }
            LocalTask localTask = findTask(id);
            if (localTask != null) {
                localTask.updateFromIssue(issue);
                return localTask;
            }
            return issue;
        });
    }

    @Override
    public List<LocalTask> getLocalTasks() {
        return getLocalTasks(true);
    }

    @Override
    public List<LocalTask> getLocalTasks(boolean withClosed) {
        synchronized (myTasks) {
            return ContainerUtil.filter(myTasks.values(), task -> withClosed || !isLocallyClosed(task));
        }
    }

    @Override
    public LocalTask addTask(Task issue) {
        LocalTaskImpl task = issue instanceof LocalTaskImpl localTask ? localTask : new LocalTaskImpl(issue);
        addTask(task);
        return task;
    }

    @Override
    public LocalTaskImpl createLocalTask(String summary) {
        return createTask(LOCAL_TASK_ID_FORMAT.format(myConfig.localTasksCounter++), summary);
    }

    private static LocalTaskImpl createTask(String id, String summary) {
        LocalTaskImpl task = new LocalTaskImpl(id, summary);
        Date date = new Date();
        task.setCreated(date);
        task.setUpdated(date);
        return task;
    }

    @Override
    public LocalTask activateTask(Task origin, boolean clearContext) {
        LocalTask activeTask = getActiveTask();
        if (origin.equals(activeTask)) {
            return activeTask;
        }

        saveActiveTask();

        if (clearContext) {
            myContextManager.clearContext();
        }
        myContextManager.restoreContext(origin);

        LocalTask task = doActivate(origin, true);

        return restoreVcsContext(task);
    }

    private LocalTask restoreVcsContext(LocalTask task) {
        if (!isVcsEnabled()) {
            return task;
        }

        List<ChangeListInfo> changeLists = task.getChangeLists();
        if (!changeLists.isEmpty()) {
            ChangeListInfo info = changeLists.get(0);
            LocalChangeList changeList = myChangeListManager.getChangeList(info.id);
            if (changeList == null) {
                changeList = myChangeListManager.addChangeList(info.name, info.comment);
                info.id = changeList.getId();
            }
            myChangeListManager.setDefaultChangeList(changeList);
        }

        List<BranchInfo> branches = task.getBranches(false);
        // we should have exactly one branch per repo
        MultiMap<String, BranchInfo> multiMap = new MultiMap<>();
        for (BranchInfo branch : branches) {
            multiMap.putValue(branch.repository, branch);
        }
        for (String repo : multiMap.keySet()) {
            Collection<BranchInfo> infos = multiMap.get(repo);
            if (infos.size() > 1) {
                // cleanup needed
                List<BranchInfo> existing = getAllBranches(repo);
                for (Iterator<BranchInfo> iterator = infos.iterator(); iterator.hasNext(); ) {
                    BranchInfo info = iterator.next();
                    if (!existing.contains(info)) {
                        iterator.remove();
                        if (infos.size() == 1) {
                            break;
                        }
                    }
                }
            }
        }

        VcsTaskHandler.TaskInfo info = fromBranches(new ArrayList<>(multiMap.values()));

        switchBranch(info);
        return task;
    }

    private List<BranchInfo> getAllBranches(String repo) {
        ArrayList<BranchInfo> infos = new ArrayList<>();
        VcsTaskHandler[] handlers = VcsTaskHandler.getAllHandlers(myProject);
        for (VcsTaskHandler handler : handlers) {
            VcsTaskHandler.TaskInfo[] tasks = handler.getAllExistingTasks();
            for (VcsTaskHandler.TaskInfo info : tasks) {
                infos.addAll(ContainerUtil.filter(BranchInfo.fromTaskInfo(info, false), info1 -> Comparing.equal(info1.repository, repo)));
            }
        }
        return infos;
    }

    private void switchBranch(VcsTaskHandler.TaskInfo info) {
        VcsTaskHandler[] handlers = VcsTaskHandler.getAllHandlers(myProject);
        for (VcsTaskHandler handler : handlers) {
            handler.switchToTask(info, null);
        }
    }

    private static VcsTaskHandler.TaskInfo fromBranches(List<BranchInfo> branches) {
        if (branches.isEmpty()) {
            return new VcsTaskHandler.TaskInfo(null, Collections.<String>emptyList());
        }
        MultiMap<String, String> map = new MultiMap<>();
        for (BranchInfo branch : branches) {
            map.putValue(branch.name, branch.repository);
        }
        Map.Entry<String, Collection<String>> next = map.entrySet().iterator().next();
        return new VcsTaskHandler.TaskInfo(next.getKey(), next.getValue());
    }

    @RequiredUIAccess
    public void createBranch(LocalTask task, LocalTask previousActive, String name, VcsTaskHandler.@Nullable TaskInfo branchFrom) {
        VcsTaskHandler[] handlers = VcsTaskHandler.getAllHandlers(myProject);
        for (VcsTaskHandler handler : handlers) {
            VcsTaskHandler.TaskInfo[] info = handler.getCurrentTasks();
            if (previousActive != null && previousActive.getBranches(false).isEmpty()) {
                addBranches(previousActive, info, false);
            }
            addBranches(task, info, true);
            if (info.length == 0 && branchFrom != null) {
                addBranches(task, new VcsTaskHandler.TaskInfo[]{branchFrom}, true);
            }
            handler.startNewTask(name).whenComplete((taskInfo, error) -> {
                if (taskInfo != null) {
                    addBranches(task, new VcsTaskHandler.TaskInfo[]{taskInfo}, false);
                }
                else if (error != null) {
                    LOG.error(error);
                }
            });
        }
    }

    public void mergeBranch(LocalTask task) {
        VcsTaskHandler.TaskInfo original = fromBranches(task.getBranches(true));
        VcsTaskHandler.TaskInfo feature = fromBranches(task.getBranches(false));

        VcsTaskHandler[] handlers = VcsTaskHandler.getAllHandlers(myProject);
        for (VcsTaskHandler handler : handlers) {
            handler.closeTask(feature, original);
        }
    }

    @Override
    public void addBranches(LocalTask task, VcsTaskHandler.TaskInfo[] info, boolean original) {
        for (VcsTaskHandler.TaskInfo taskInfo : info) {
            List<BranchInfo> branchInfos = BranchInfo.fromTaskInfo(taskInfo, original);
            for (BranchInfo branchInfo : branchInfos) {
                task.addBranch(branchInfo);
            }
        }
    }

    private void saveActiveTask() {
        myContextManager.saveContext(myActiveTask);
        myActiveTask.setUpdated(new Date());
    }

    private LocalTask doActivate(Task origin, boolean explicitly) {
        final LocalTaskImpl task = origin instanceof LocalTaskImpl localTask ? localTask : new LocalTaskImpl(origin);
        if (explicitly) {
            task.setUpdated(new Date());
        }
        myActiveTask.setActive(false);
        task.setActive(true);
        addTask(task);
        if (task.isIssue()) {
            StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> refreshIssue(task.getId()));
        }
        LocalTask oldActiveTask = myActiveTask;
        boolean isChanged = !task.equals(oldActiveTask);
        myActiveTask = task;
        if (isChanged) {
            myDispatcher.getMulticaster().taskDeactivated(oldActiveTask);
            myDispatcher.getMulticaster().taskActivated(task);
        }
        return task;
    }

    private void addTask(LocalTaskImpl task) {
        myTasks.put(task.getId(), task);
        myDispatcher.getMulticaster().taskAdded(task);
    }

    public CompletableFuture<?> setTaskState(TaskRepository repository, Task task, CustomTaskState state, boolean open) {
        IssueStateSupport support = repository.getIssueTracker().getSupport(IssueStateSupport.class);
        if (support == null) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Setting task state is not supported by " + repository));
        }
        return support.setState(task, state).thenAccept(result -> {
            if (open) {
                repository.getOptions().preferredOpenTaskState = state;
            }
            else {
                repository.getOptions().preferredCloseTaskState = state;
            }
        });
    }

    @Override
    public CompletableFuture<?> testConnection(TaskRepository repository) {
        return repository.getIssueTracker().search(new IssueQuery(null, false), 0, 1).thenAccept(page -> myBadRepositories.remove(repository));
    }

    @Override
    public Config getState() {
        myConfig.tasks = ContainerUtil.map(myTasks.values(), LocalTaskImpl::new);
        myConfig.servers = myStore.write();
        return myConfig;
    }

    @Override
    public void loadState(Config config) {
        XmlSerializerUtil.copyBean(config, myConfig);
        myTasks.clear();
        for (LocalTaskImpl task : config.tasks) {
            addTask(task);
        }

        myStore.read(config.servers, this::findRepositoryType);
        rebuildRepositories();
    }

    public void projectOpened() {
        myAccountManager.addListener(new AccountListener() {
            @Override
            public void accountsChanged() {
                rebuildRepositories();
            }

            @Override
            public void accountChanged(Account account) {
                rebuildRepositories();
            }
        }, this);
        rebuildRepositories();

        myContextManager.pack(200, 50);

        // make sure the task is associated with default changelist
        LocalTask defaultTask = findTask(LocalTaskImpl.DEFAULT_TASK_ID);
        LocalChangeList defaultList = myChangeListManager.findChangeList(LocalChangeList.DEFAULT_NAME);
        if (defaultList != null && defaultTask != null) {
            ChangeListInfo listInfo = new ChangeListInfo(defaultList);
            if (!defaultTask.getChangeLists().contains(listInfo)) {
                defaultTask.addChangelist(listInfo);
            }
        }

        // remove already not existing changelists from tasks changelists
        for (LocalTask localTask : getLocalTasks()) {
            for (Iterator<ChangeListInfo> iterator = localTask.getChangeLists().iterator(); iterator.hasNext(); ) {
                ChangeListInfo changeListInfo = iterator.next();
                if (myChangeListManager.getChangeList(changeListInfo.id) == null) {
                    iterator.remove();
                }
            }
        }

        myChangeListManager.addChangeListListener(myChangeListListener);
    }

    private TaskProjectConfiguration getProjectConfiguration() {
        return myProject.getInstance(TaskProjectConfiguration.class);
    }

    public void startUpdate() {
        myCacheRefreshTimer = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
            if (myConfig.updateEnabled && !myUpdating) {
                LOG.info("Updating issues cache (every " + myConfig.updateInterval + " min)");
                updateIssues(null);
            }
        }, 0, myConfig.updateInterval, TimeUnit.MINUTES);

        // make sure that the default task is exist
        LocalTask defaultTask = findTask(LocalTaskImpl.DEFAULT_TASK_ID);
        if (defaultTask == null) {
            defaultTask = createDefaultTask();
            addTask(defaultTask);
        }

        // search for active task
        LocalTask activeTask = null;
        List<LocalTask> tasks = getLocalTasks();
        Collections.sort(tasks, TASK_UPDATE_COMPARATOR);
        for (LocalTask task : tasks) {
            if (activeTask == null) {
                if (task.isActive()) {
                    activeTask = task;
                }
            }
            else {
                task.setActive(false);
            }
        }
        if (activeTask == null) {
            activeTask = defaultTask;
        }

        myActiveTask = activeTask;
        doActivate(myActiveTask, false);
        myDispatcher.getMulticaster().taskActivated(myActiveTask);
    }

    private static LocalTaskImpl createDefaultTask() {
        LocalTaskImpl task = new LocalTaskImpl(LocalTaskImpl.DEFAULT_TASK_ID, "Default task");
        Date date = new Date();
        task.setCreated(date);
        task.setUpdated(date);
        return task;
    }

    @Override
    public void dispose() {
        if (myCacheRefreshTimer != null) {
            myCacheRefreshTimer.cancel(false);
            myCacheRefreshTimer = null;
        }
        myChangeListManager.removeChangeListListener(myChangeListListener);
    }

    @Override
    public void updateIssues(@Nullable Runnable onComplete) {
        TaskRepository first = ContainerUtil.find(getAllRepositories(), TaskRepository::isConfigured);
        if (first == null) {
            myIssueCache.clear();
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        myUpdating = true;
        searchRepositories(null, 0, myConfig.updateIssuesCount, false, false).whenComplete((issues, error) -> {
            try {
                if (issues == null) {
                    return;
                }
                synchronized (myIssueCache) {
                    myIssueCache.clear();
                    for (Task issue : issues) {
                        myIssueCache.put(issue.getId(), issue);
                    }
                }
                synchronized (myTasks) {
                    for (Map.Entry<String, LocalTask> entry : myTasks.entrySet()) {
                        Task issue = myIssueCache.get(entry.getKey());
                        if (issue != null) {
                            entry.getValue().updateFromIssue(issue);
                        }
                    }
                }
            }
            finally {
                if (onComplete != null) {
                    onComplete.run();
                }
                myUpdating = false;
            }
        });
    }

    private CompletableFuture<@Nullable List<Task>> searchRepositories(
        @Nullable String request,
        int offset,
        int limit,
        boolean withClosed,
        boolean forceRequest
    ) {
        List<CompletableFuture<@Nullable List<Task>>> futures = new ArrayList<>();
        for (TaskRepository repository : getAllRepositories()) {
            if (!repository.isConfigured() || (!forceRequest && myBadRepositories.contains(repository))) {
                continue;
            }
            IssueTracker tracker = repository.getIssueTracker();
            futures.add(tracker.search(new IssueQuery(request, withClosed), offset, limit).handle((page, error) -> {
                if (error != null) {
                    Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                    LOG.warn("Cannot connect to " + repository + ": " + cause.getMessage());
                    myBadRepositories.add(repository);
                    if (forceRequest) {
                        notifyAboutConnectionFailure(repository, StringUtil.notNullize(cause.getMessage()));
                    }
                    return null;
                }
                myBadRepositories.remove(repository);
                List<Task> tasks = page.getIssues();
                if (!tracker.getCapabilities().contains(IssueCapability.NATIVE_SEARCH) && request != null) {
                    return TaskSearchSupport.filterTasks(request, tasks);
                }
                return tasks;
            }));
        }
        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            List<Task> issues = null;
            for (CompletableFuture<@Nullable List<Task>> future : futures) {
                List<Task> tasks = future.join();
                if (tasks != null) {
                    if (issues == null) {
                        issues = new ArrayList<>();
                    }
                    issues.addAll(tasks);
                }
            }
            return issues;
        });
    }

    private void notifyAboutConnectionFailure(TaskRepository repository, String details) {
        String content = "<p><a href=\"\">Configure server...</a></p>";
        if (!StringUtil.isEmpty(details)) {
            content = "<p>" + details + "</p>" + content;
        }
        myNotificationService.newWarn(TaskManagerNotificationGroupContributor.TASKS_NOTIFICATION_GROUP)
            .title(LocalizeValue.localizeTODO("Cannot connect to " + repository.getServerUrl()))
            .content(LocalizeValue.localizeTODO(content))
            .hyperlinkListener((notification, event) -> {
                Application.get().getInstance(ShowConfigurableService.class).showAndSelect(myProject, TaskRepositoriesConfigurable.class);
                notification.expire();
            })
            .notify(myProject);
    }

    @Override
    public boolean isVcsEnabled() {
        return ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss().length > 0;
    }

    @Override
    public AbstractVcs getActiveVcs() {
        AbstractVcs[] vcss = ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss();
        if (vcss.length == 0) {
            return null;
        }
        for (AbstractVcs vcs : vcss) {
            if (vcs.getType() == VcsType.distributed) {
                return vcs;
            }
        }
        return vcss[0];
    }

    @Override
    public boolean isLocallyClosed(LocalTask localTask) {
        if (isVcsEnabled()) {
            List<ChangeListInfo> lists = localTask.getChangeLists();
            if (lists.isEmpty()) {
                return true;
            }
            for (ChangeListInfo list : lists) {
                if (StringUtil.isEmpty(list.id)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public @Nullable LocalTask getAssociatedTask(LocalChangeList list) {
        for (LocalTask task : getLocalTasks()) {
            for (ChangeListInfo changeListInfo : task.getChangeLists()) {
                if (changeListInfo.id.equals(list.getId())) {
                    return task;
                }
            }
        }
        return null;
    }

    @Override
    public void trackContext(LocalChangeList changeList) {
        ChangeListInfo changeListInfo = new ChangeListInfo(changeList);
        String changeListName = changeList.getName();
        LocalTaskImpl task = createLocalTask(changeListName);
        task.addChangelist(changeListInfo);
        addTask(task);
        if (changeList.isDefault()) {
            activateTask(task, false);
        }
    }

    @Override
    public void disassociateFromTask(LocalChangeList changeList) {
        ChangeListInfo changeListInfo = new ChangeListInfo(changeList);
        for (LocalTask localTask : getLocalTasks()) {
            if (localTask.getChangeLists().contains(changeListInfo)) {
                localTask.removeChangelist(changeListInfo);
            }
        }
    }

    public void createChangeList(LocalTask task, String name) {
        String comment = TaskUtil.getChangeListComment(task);
        createChangeList(task, name, comment);
    }

    private void createChangeList(LocalTask task, String name, @Nullable String comment) {
        LocalChangeList changeList = myChangeListManager.findChangeList(name);
        if (changeList == null) {
            changeList = myChangeListManager.addChangeList(name, comment);
        }
        else {
            LocalTask associatedTask = getAssociatedTask(changeList);
            if (associatedTask != null) {
                associatedTask.removeChangelist(new ChangeListInfo(changeList));
            }
            changeList.setComment(comment);
        }
        task.addChangelist(new ChangeListInfo(changeList));
        myChangeListManager.setDefaultChangeList(changeList);
    }

    public String getChangelistName(Task task) {
        String name = task.isIssue() && myConfig.changelistNameFormat != null ? TaskUtil.formatTask(
            task,
            myConfig.changelistNameFormat
        ) : task.getSummary();
        return StringUtil.shortenTextWithEllipsis(name, 100, 0);
    }

    @Override

    public String constructDefaultBranchName(Task task) {
        return task.isIssue() ? TaskUtil.formatTask(task, myConfig.branchNameFormat) : task.getSummary();
    }

    @Override
    public boolean isTrackContextForNewChangelist() {
        return myConfig.trackContextForNewChangelist;
    }

    @Override
    public void setTrackContextForNewChangelist(boolean value) {
        myConfig.trackContextForNewChangelist = value;
    }

    public static class Config {
        @Property(surroundWithTag = false)
        @AbstractCollection(surroundWithTag = false, elementTag = "task")
        public List<LocalTaskImpl> tasks = new ArrayList<>();

        public int localTasksCounter = 1;

        public int taskHistoryLength = 50;

        public boolean updateEnabled = true;
        public int updateInterval = 20;
        public int updateIssuesCount = 100;

        // create task options
        public boolean clearContext = true;
        public boolean createChangelist = true;
        public boolean createBranch = true;
        public boolean useBranch = false;

        // close task options
        public boolean commitChanges = true;
        public boolean mergeBranch = true;

        public boolean saveContextOnCommit = true;
        public boolean trackContextForNewChangelist = false;

        public String changelistNameFormat = "{id} {summary}";
        public String branchNameFormat = "{id}";

        public boolean searchClosedTasks = false;
        @Tag("servers")
        public Element servers = new Element("servers");
    }
}
