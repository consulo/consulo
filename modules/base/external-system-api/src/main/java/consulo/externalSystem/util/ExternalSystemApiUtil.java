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
package consulo.externalSystem.util;

import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.Semaphore;
import consulo.content.OrderRootType;
import consulo.content.library.Library;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.event.ExecutionListener;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.runner.RunnerRegistry;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.internal.ui.ExternalSystemRecentTasksList;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.externalSystem.model.project.LibraryData;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.externalSystem.model.task.TaskCallback;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import consulo.externalSystem.service.execution.ExternalSystemRunConfiguration;
import consulo.externalSystem.service.module.extension.ExternalSystemModuleExtension;
import consulo.externalSystem.service.project.autoimport.ExternalSystemAutoImportAware;
import consulo.externalSystem.setting.AbstractExternalSystemLocalSettings;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.io.ClassPathUtil;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.virtualFileSystem.util.PathsList;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Denis Zhdanov
 * @since 2013-04-01
 */
public class ExternalSystemApiUtil {
    private static final Logger LOG = Logger.getInstance(ExternalSystemApiUtil.class);
    private static final String LAST_USED_PROJECT_PATH_PREFIX = "LAST_EXTERNAL_PROJECT_PATH_";

    
    private static final Map<String, String> RUNNER_IDS = new HashMap<>();

    static {
        RUNNER_IDS.put(DefaultRunExecutor.EXECUTOR_ID, ExternalSystemConstants.RUNNER_ID);
        RUNNER_IDS.put(DefaultDebugExecutor.EXECUTOR_ID, ExternalSystemConstants.DEBUG_RUNNER_ID);
    }

    public interface TaskUnderProgress {
        void execute(ProgressIndicator indicator);
    }

    
    public static final String PATH_SEPARATOR = "/";

    
    private static final Pattern ARTIFACT_PATTERN = Pattern.compile("(?:.*/)?(.+?)(?:-([\\d+](?:\\.[\\d]+)*))?(?:\\.[^\\.]+?)?");

    
    public static final Comparator<Object> ORDER_AWARE_COMPARATOR = new Comparator<>() {
        @Override
        public int compare(Object o1, Object o2) {
            int order1 = getOrder(o1);
            int order2 = getOrder(o2);
            return order1 < order2 ? -1 : (order1 == order2 ? 0 : 1);
        }

        private int getOrder(Object o) {
            Queue<Class<?>> toCheck = new ArrayDeque<>();
            toCheck.add(o.getClass());
            while (!toCheck.isEmpty()) {
                Class<?> clazz = toCheck.poll();
                Order annotation = clazz.getAnnotation(Order.class);
                if (annotation != null) {
                    return annotation.value();
                }
                Class<?> c = clazz.getSuperclass();
                if (c != null) {
                    toCheck.add(c);
                }
                Class<?>[] interfaces = clazz.getInterfaces();
                Collections.addAll(toCheck, interfaces);
            }
            return ExternalSystemConstants.UNORDERED;
        }
    };

    
    private static final Function<DataNode<?>, Key<?>> GROUPER = DataNode::getKey;

    
    private static final Comparator<Object> COMPARABLE_GLUE = (o1, o2) -> ((Comparable)o1).compareTo(o2);

    private ExternalSystemApiUtil() {
    }

    public static @Nullable String getRunnerId(String executorId) {
        return RUNNER_IDS.get(executorId);
    }

    @RequiredUIAccess
    public static void runTask(
        ExternalSystemTaskExecutionSettings taskSettings,
        String executorId,
        Project project,
        ProjectSystemId externalSystemId
    ) {
        runTask(taskSettings, executorId, project, externalSystemId, null, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
    }

    @RequiredUIAccess
    public static void runTask(
        ExternalSystemTaskExecutionSettings taskSettings,
        String executorId,
        Project project,
        ProjectSystemId externalSystemId,
        @Nullable TaskCallback callback,
        ProgressExecutionMode progressExecutionMode
    ) {
        Pair<ProgramRunner, ExecutionEnvironment> pair = createRunner(taskSettings, executorId, project, externalSystemId);
        if (pair == null) {
            return;
        }

        ProgramRunner runner = pair.first;
        ExecutionEnvironment environment = pair.second;

        @RequiredUIAccess
        TaskUnderProgress task = indicator -> {
            Semaphore targetDone = new Semaphore();
            SimpleReference<Boolean> result = new SimpleReference<>(false);
            Disposable disposable = Disposable.newDisposable();

            project.getMessageBus().connect(disposable).subscribe(ExecutionListener.class, new ExecutionListener() {
                @Override
                public void processStartScheduled(String executorIdLocal, ExecutionEnvironment environmentLocal) {
                    if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                        targetDone.down();
                    }
                }

                @Override
                public void processNotStarted(String executorIdLocal, ExecutionEnvironment environmentLocal) {
                    if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                        targetDone.up();
                    }
                }

                @Override
                public void processStarted(
                    String executorIdLocal,
                    ExecutionEnvironment environmentLocal,
                    ProcessHandler handler
                ) {
                    if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                        handler.addProcessListener(new ProcessAdapter() {
                            @Override
                            public void processTerminated(ProcessEvent event) {
                                result.set(event.getExitCode() == 0);
                                targetDone.up();
                            }
                        });
                    }
                }
            });

            try {
                Application app = Application.get();
                app.invokeAndWait(
                    () -> {
                        try {
                            runner.execute(environment);
                        }
                        catch (ExecutionException e) {
                            targetDone.up();
                            LOG.error(e);
                        }
                    },
                    app.getNoneModalityState()
                );
            }
            catch (Exception e) {
                LOG.error(e);
                Disposer.dispose(disposable);
                return;
            }

            targetDone.waitFor();
            Disposer.dispose(disposable);

            if (callback != null) {
                if (result.get()) {
                    callback.onSuccess();
                }
                else {
                    callback.onFailure();
                }
            }
        };

        UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
            String title = AbstractExternalSystemTaskConfigurationType.generateName(project, taskSettings);
            switch (progressExecutionMode) {
                case MODAL_SYNC:
                    new Task.Modal(project, title, true) {
                        @Override
                        public void run(ProgressIndicator indicator) {
                            task.execute(indicator);
                        }
                    }.queue();
                    break;
                case IN_BACKGROUND_ASYNC:
                    new Task.Backgroundable(project, title) {
                        @Override
                        public void run(ProgressIndicator indicator) {
                            task.execute(indicator);
                        }
                    }.queue();
                    break;
                case START_IN_FOREGROUND_ASYNC:
                    new Task.Backgroundable(project, title, true, PerformInBackgroundOption.DEAF) {
                        @Override
                        public void run(ProgressIndicator indicator) {
                            task.execute(indicator);
                        }
                    }.queue();
            }
        });
    }

    public static @Nullable AbstractExternalSystemTaskConfigurationType findConfigurationType(ProjectSystemId externalSystemId) {
        return Application.get().getExtensionPoint(ConfigurationType.class).computeSafeIfAny(
            type -> type instanceof AbstractExternalSystemTaskConfigurationType candidate
                && externalSystemId.equals(candidate.getExternalSystemId()) ? candidate : null
        );
    }

    public static @Nullable Pair<ProgramRunner, ExecutionEnvironment> createRunner(
        ExternalSystemTaskExecutionSettings taskSettings,
        String executorId,
        Project project,
        ProjectSystemId externalSystemId
    ) {
        Executor executor = ExecutorRegistry.getInstance().getExecutorById(executorId);
        if (executor == null) {
            return null;
        }

        String runnerId = getRunnerId(executorId);
        if (runnerId == null) {
            return null;
        }

        ProgramRunner runner = RunnerRegistry.getInstance().findRunnerById(runnerId);
        if (runner == null) {
            return null;
        }

        AbstractExternalSystemTaskConfigurationType configurationType = findConfigurationType(externalSystemId);
        if (configurationType == null) {
            return null;
        }

        String name = AbstractExternalSystemTaskConfigurationType.generateName(project, taskSettings);
        RunnerAndConfigurationSettings settings =
            RunManager.getInstance(project).createRunConfiguration(name, configurationType.getFactory());
        ExternalSystemRunConfiguration runConfiguration = (ExternalSystemRunConfiguration)settings.getConfiguration();
        runConfiguration.getSettings().setExternalProjectPath(taskSettings.getExternalProjectPath());
        runConfiguration.getSettings().setTaskNames(new ArrayList<>(taskSettings.getTaskNames()));
        runConfiguration.getSettings().setTaskDescriptions(new ArrayList<>(taskSettings.getTaskDescriptions()));
        runConfiguration.getSettings().setVmOptions(taskSettings.getVmOptions());
        runConfiguration.getSettings().setScriptParameters(taskSettings.getScriptParameters());
        runConfiguration.getSettings().setExecutionName(taskSettings.getExecutionName());

        return Pair.create(runner, new ExecutionEnvironment(executor, runner, settings, project));
    }

    /**
     * Is expected to be called when given task info is about to be executed.
     * <p>
     * Basically, this method updates recent tasks list at the corresponding external system tool window and
     * persists new recent tasks state.
     *
     * @param taskInfo task which is about to be executed
     * @param project  target project
     */
    public static void updateRecentTasks(ExternalTaskExecutionInfo taskInfo, Project project) {
        ExternalSystemManager<?, ?, ?, ?, ?> manager =
            ExternalSystemApiUtil.getManagerStrict(taskInfo.getSettings().getExternalSystemIdString());

        ProjectSystemId externalSystemId = manager.getSystemId();

        ExternalSystemRecentTasksList recentTasksList =
            getToolWindowElement(ExternalSystemRecentTasksList.class, project, ExternalSystemDataKeys.RECENT_TASKS_LIST, externalSystemId);
        if (recentTasksList == null) {
            return;
        }
        recentTasksList.setFirst(taskInfo);

        AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().apply(project);
        settings.setRecentTasks(recentTasksList.getModel().getTasks());
    }

    @SuppressWarnings("unchecked")
    public static @Nullable <T> T getToolWindowElement(
        Class<T> clazz,
        Project project,
        consulo.util.dataholder.Key<T> key,
        ProjectSystemId externalSystemId
    ) {
        if (project.isDisposed() || !project.isOpen()) {
            return null;
        }
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        if (toolWindowManager == null) {
            return null;
        }
        ToolWindow toolWindow = ensureToolWindowContentInitialized(project, externalSystemId);
        if (toolWindow == null) {
            return null;
        }

        ContentManager contentManager = toolWindow.getContentManager();

        for (Content content : contentManager.getContents()) {
            if (content.getComponent() instanceof DataProvider dataProvider) {
                Object data = dataProvider.getData(key);
                if (data != null && clazz.isInstance(data)) {
                    return (T)data;
                }
            }
        }
        return null;
    }

    public static @Nullable ToolWindow ensureToolWindowContentInitialized(Project project, ProjectSystemId externalSystemId) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        if (toolWindowManager == null) {
            return null;
        }

        ToolWindow toolWindow = toolWindowManager.getToolWindow(externalSystemId.getToolWindowId());
        if (toolWindow == null) {
            return null;
        }

        // call content manager - initialize it
        toolWindow.getContentManager();
        return toolWindow;
    }

    
    public static String extractNameFromPath(String path) {
        String strippedPath = stripPath(path);
        int i = strippedPath.lastIndexOf(PATH_SEPARATOR);
        String result;
        if (i < 0 || i >= strippedPath.length() - 1) {
            result = strippedPath;
        }
        else {
            result = strippedPath.substring(i + 1);
        }
        return result;
    }

    
    private static String stripPath(String path) {
        String[] endingsToStrip = {"/", "!", ".jar"};
        StringBuilder buffer = new StringBuilder(path);
        for (String ending : endingsToStrip) {
            if (buffer.lastIndexOf(ending) == buffer.length() - ending.length()) {
                buffer.setLength(buffer.length() - ending.length());
            }
        }
        return buffer.toString();
    }

    
    public static String getLibraryName(Library library) {
        String result = library.getName();
        if (result != null) {
            return result;
        }
        for (OrderRootType type : OrderRootType.getAllTypes()) {
            for (String url : library.getUrls(type)) {
                String candidate = extractNameFromPath(url);
                if (!StringUtil.isEmpty(candidate)) {
                    return candidate;
                }
            }
        }
        assert false;
        return "unknown-lib";
    }

    public static boolean isRelated(Library library, LibraryData libraryData) {
        return getLibraryName(library).equals(libraryData.getInternalName());
    }

    public static boolean isExternalSystemLibrary(Library library, ProjectSystemId externalSystemId) {
        return library.getName() != null && StringUtil.startsWith(library.getName(), externalSystemId.getLibraryPrefix() + ": ");
    }

    public static @Nullable ArtifactInfo parseArtifactInfo(String fileName) {
        Matcher matcher = ARTIFACT_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return null;
        }
        return new ArtifactInfo(matcher.group(1), null, matcher.group(2));
    }

    @Deprecated
    public static void orderAwareSort(List<?> data) {
        Collections.sort(data, ORDER_AWARE_COMPARATOR);
    }

    /**
     * @param path target path
     * @return absolute path that points to the same location as the given one and that uses only slashes
     */
    
    public static String toCanonicalPath(String path) {
        String p = normalizePath(new File(path).getAbsolutePath());
        assert p != null;
        return PathUtil.getCanonicalPath(p);
    }

    
    public static String getLocalFileSystemPath(VirtualFile file) {
        VirtualFile archiveRoot = ArchiveVfsUtil.getVirtualFileForArchive(file);
        if (archiveRoot != null) {
            return archiveRoot.getPath();
        }
        return toCanonicalPath(file.getPath());
    }

    
    public static ExternalSystemManager<?, ?, ?, ?, ?> getManagerStrict(String externalSystemId) {
        ExternalSystemManager<?, ?, ?, ?, ?> manager = getManager(externalSystemId);
        if (manager != null) {
            return manager;
        }

        throw new IllegalArgumentException("There no " + ExternalSystemManager.class.getName() + " for id: " + externalSystemId);
    }

    public static @Nullable ExternalSystemManager<?, ?, ?, ?, ?> getManager(String externalSystemId) {
        return Application.get().getExtensionPoint(ExternalSystemManager.class)
            .findFirstSafe(manager -> Objects.equals(externalSystemId, manager.getSystemId().getId()));
    }

    public static @Nullable ExternalSystemManager<?, ?, ?, ?, ?> getManager(ProjectSystemId externalSystemId) {
        return getManager(externalSystemId.getId());
    }

    
    public static Map<Key<?>, List<DataNode<?>>> group(Collection<DataNode<?>> nodes) {
        return groupBy(nodes, GROUPER);
    }

    
    public static <K, V> Map<DataNode<K>, List<DataNode<V>>> groupBy(Collection<DataNode<V>> nodes, Key<K> key) {
        return groupBy(nodes, new Function<DataNode<V>, DataNode<K>>() {
            @Nullable
            @Override
            public DataNode<K> apply(DataNode<V> node) {
                return node.getDataNode(key);
            }
        });
    }

    
    public static <K, V> Map<K, List<V>> groupBy(Collection<V> nodes, Function<V, K> grouper) {
        Map<K, List<V>> result = new HashMap<>();
        for (V data : nodes) {
            K key = grouper.apply(data);
            if (key == null) {
                LOG.warn(String.format(
                    "Skipping entry '%s' during grouping. Reason: it's not possible to build a grouping key with grouping strategy '%s'. " + "Given entries: %s",
                    data,
                    grouper.getClass(),
                    nodes
                ));
                continue;
            }
            List<V> grouped = result.get(key);
            if (grouped == null) {
                result.put(key, grouped = new ArrayList<>());
            }
            grouped.add(data);
        }

        if (!result.isEmpty() && result.keySet().iterator().next() instanceof Comparable) {
            List<K> ordered = new ArrayList<>(result.keySet());
            Collections.sort(ordered, COMPARABLE_GLUE);
            Map<K, List<V>> orderedResult = new LinkedHashMap<>();
            for (K k : ordered) {
                orderedResult.put(k, result.get(k));
            }
            return orderedResult;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    
    public static <T> Collection<DataNode<T>> getChildren(DataNode<?> node, Key<T> key) {
        Collection<DataNode<T>> result = null;
        for (DataNode<?> child : node.getChildren()) {
            if (!key.equals(child.getKey())) {
                continue;
            }
            if (result == null) {
                result = new ArrayList<>();
            }
            result.add((DataNode<T>)child);
        }
        return result == null ? Collections.<DataNode<T>>emptyList() : result;
    }

    @SuppressWarnings("unchecked")
    public static @Nullable <T> DataNode<T> find(DataNode<?> node, Key<T> key) {
        for (DataNode<?> child : node.getChildren()) {
            if (key.equals(child.getKey())) {
                return (DataNode<T>)child;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static @Nullable <T> DataNode<T> find(DataNode<?> node, Key<T> key, Predicate<DataNode<T>> predicate) {
        for (DataNode<?> child : node.getChildren()) {
            if (key.equals(child.getKey()) && predicate.test((DataNode<T>)child)) {
                return (DataNode<T>)child;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static @Nullable <T> DataNode<T> findParent(DataNode<?> node, Key<T> key) {
        return findParent(node, key, null);
    }

    @SuppressWarnings("unchecked")
    public static @Nullable <T> DataNode<T> findParent(DataNode<?> node, Key<T> key, @Nullable Predicate<DataNode<T>> predicate) {
        DataNode<?> parent = node.getParent();
        if (parent == null) {
            return null;
        }
        return key.equals(parent.getKey())
            && (predicate == null || predicate.test((DataNode<T>)parent)) ? (DataNode<T>)parent : findParent(parent, key, predicate);
    }

    @SuppressWarnings("unchecked")
    
    public static <T> Collection<DataNode<T>> findAll(DataNode<?> parent, Key<T> key) {
        Collection<DataNode<T>> result = null;
        for (DataNode<?> child : parent.getChildren()) {
            if (!key.equals(child.getKey())) {
                continue;
            }
            if (result == null) {
                result = new ArrayList<>();
            }
            result.add((DataNode<T>)child);
        }
        return result == null ? Collections.<DataNode<T>>emptyList() : result;
    }

    @RequiredUIAccess
    public static void executeProjectChangeAction(DisposeAwareProjectChange task) {
        executeProjectChangeAction(false, task);
    }

    @RequiredUIAccess
    public static void executeProjectChangeAction(boolean synchronous, DisposeAwareProjectChange task) {
        executeOnEdt(synchronous, () -> Application.get().runWriteAction(task));
    }

    @RequiredUIAccess
    public static void executeOnEdt(boolean synchronous, @RequiredUIAccess Runnable task) {
        Application app = Application.get();
        if (app.isDispatchThread()) {
            task.run();
            return;
        }

        if (synchronous) {
            app.invokeAndWait(task);
        }
        else {
            app.invokeLater(task);
        }
    }

    @RequiredUIAccess
    public static <T> T executeOnEdt(@RequiredUIAccess Supplier<T> task) {
        Application app = Application.get();
        SimpleReference<T> result = SimpleReference.create();
        app.invokeAndWait(() -> result.set(task.get()));
        return result.get();
    }

    @RequiredUIAccess
    public static <T> T doWriteAction(Supplier<T> task) {
        return executeOnEdt(() -> Application.get().runWriteAction(task));
    }

    @RequiredUIAccess
    public static void doWriteAction(Runnable task) {
        executeOnEdt(true, () -> Application.get().runWriteAction(task));
    }

    /**
     * Adds runnable to Event Dispatch Queue
     * if we aren't in UnitTest of Headless environment mode
     *
     * @param runnable Runnable
     */
    public static void addToInvokeLater(Runnable runnable) {
        Application app = Application.get();
        if (app.isHeadlessEnvironment() || app.isDispatchThread()) {
            runnable.run();
        }
        else {
            app.getLastUIAccess().giveIfNeed(runnable);
        }
    }

    /**
     * Configures given classpath to reference target i18n bundle file(s).
     *
     * @param classPath    process classpath
     * @param bundlePath   path to the target bundle file
     * @param contextClass class from the same content root as the target bundle file
     */
    public static void addBundle(PathsList classPath, String bundlePath, Class<?> contextClass) {
        String pathToUse = bundlePath.replace('.', '/');
        if (!pathToUse.endsWith(".properties")) {
            pathToUse += ".properties";
        }
        if (!pathToUse.startsWith("/")) {
            pathToUse = '/' + pathToUse;
        }
        String root = ClassPathUtil.getResourceRoot(contextClass, pathToUse);
        if (root != null) {
            classPath.add(root);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static @Nullable String normalizePath(@Nullable String s) {
        return StringUtil.isEmpty(s) ? null : s.replace('\\', ExternalSystemConstants.PATH_SEPARATOR);
    }

    /**
     * We can divide all 'import from external system' use-cases into at least as below:
     * <pre>
     * <ul>
     *   <li>this is a new project being created (import project from external model);</li>
     *   <li>a new module is being imported from an external project into an existing ide project;</li>
     * </ul>
     * </pre>
     * This method allows to differentiate between them (e.g. we don't want to change language level when new module is imported to
     * an existing project).
     *
     * @return <code>true</code> if new project is being imported; <code>false</code> if new module is being imported
     */
    public static boolean isNewProjectConstruction() {
        return ProjectManager.getInstance().getOpenProjects().length == 0;
    }

//    @NotNull
//    public static String getLastUsedExternalProjectPath(@NotNull ProjectSystemId externalSystemId) {
//        return PropertiesComponent.getInstance().getValue(LAST_USED_PROJECT_PATH_PREFIX + externalSystemId.getReadableName(), "");
//    }

    public static void storeLastUsedExternalProjectPath(@Nullable String path, ProjectSystemId externalSystemId) {
        if (path != null) {
            ApplicationPropertiesComponent.getInstance().setValue(LAST_USED_PROJECT_PATH_PREFIX + externalSystemId.getId(), path);
        }
    }

    
    public static String getProjectRepresentationName(String targetProjectPath, @Nullable String rootProjectPath) {
        if (rootProjectPath == null) {
            File rootProjectDir = new File(targetProjectPath);
            if (rootProjectDir.isFile()) {
                rootProjectDir = rootProjectDir.getParentFile();
            }
            return rootProjectDir.getName();
        }
        File rootProjectDir = new File(rootProjectPath);
        if (rootProjectDir.isFile()) {
            rootProjectDir = rootProjectDir.getParentFile();
        }
        File targetProjectDir = new File(targetProjectPath);
        if (targetProjectDir.isFile()) {
            targetProjectDir = targetProjectDir.getParentFile();
        }
        StringBuilder buffer = new StringBuilder();
        for (File f = targetProjectDir; f != null && !FileUtil.filesEqual(f, rootProjectDir); f = f.getParentFile()) {
            buffer.insert(0, f.getName()).insert(0, ":");
        }
        buffer.insert(0, rootProjectDir.getName());
        return buffer.toString();
    }

    /**
     * There is a possible case that external project linked to an ide project is a multi-project, i.e. contains more than one
     * module.
     * <p>
     * This method tries to find root project's config path assuming that given path points to a sub-project's config path.
     *
     * @param externalProjectPath external sub-project's config path
     * @param externalSystemId    target external system
     * @param project             target ide project
     * @return root external project's path if given path is considered to point to a known sub-project's config;
     * <code>null</code> if it's not possible to find a root project's config path on the basis of the
     * given path
     */
    public static @Nullable String getRootProjectPath(
        String externalProjectPath,
        ProjectSystemId externalSystemId,
        Project project
    ) {
        ExternalSystemManager<?, ?, ?, ?, ?> manager = getManager(externalSystemId);
        if (manager == null) {
            return null;
        }
        if (manager instanceof ExternalSystemAutoImportAware autoImportAware) {
            return autoImportAware.getAffectedExternalProjectPath(externalProjectPath, project);
        }
        return null;
    }

    /**
     * Unwraps given exception if possible and builds error message for it.
     *
     * @param e exception to process
     * @return error message for the given exception
     */
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "IOResourceOpenedButNotSafelyClosed"})

    public static String buildErrorMessage(Throwable e) {
        Throwable unwrapped = unwrapException(e);
        String reason = unwrapped.getLocalizedMessage();
        if (!StringUtil.isEmpty(reason)) {
            return reason;
        }
        else if (unwrapped instanceof ExternalSystemException externalSystemException) {
            return String.format(
                "exception during working with external system: %s",
                externalSystemException.getOriginalReason()
            );
        }
        else {
            StringWriter writer = new StringWriter();
            unwrapped.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        }
    }

    @SuppressWarnings("unchecked")
    
    public static AbstractExternalSystemSettings getSettings(
        Project project,
        ProjectSystemId externalSystemId
    ) throws IllegalArgumentException {
        ExternalSystemManager<?, ?, ?, ?, ?> manager = getManager(externalSystemId);
        if (manager == null) {
            throw new IllegalArgumentException(String.format(
                "Can't retrieve external system settings for id '%s'. Reason: no such external system is registered",
                externalSystemId.getDisplayName()
            ));
        }
        return manager.getSettingsProvider().apply(project);
    }

    @SuppressWarnings("unchecked")
    public static <S extends AbstractExternalSystemLocalSettings> S getLocalSettings(
        Project project,
        ProjectSystemId externalSystemId
    ) throws IllegalArgumentException {
        ExternalSystemManager<?, ?, ?, ?, ?> manager = getManager(externalSystemId);
        if (manager == null) {
            throw new IllegalArgumentException(String.format(
                "Can't retrieve local external system settings for id '%s'. Reason: no such external system is registered",
                externalSystemId.getDisplayName()
            ));
        }
        return (S)manager.getLocalSettingsProvider().apply(project);
    }

    @SuppressWarnings("unchecked")
    public static <S extends ExternalSystemExecutionSettings> S getExecutionSettings(
        Project project,
        String linkedProjectPath,
        ProjectSystemId externalSystemId
    )
        throws IllegalArgumentException {
        ExternalSystemManager<?, ?, ?, ?, ?> manager = getManager(externalSystemId);
        if (manager == null) {
            throw new IllegalArgumentException(String.format(
                "Can't retrieve external system execution settings for id '%s'. Reason: no such external system is registered",
                externalSystemId.getDisplayName()
            ));
        }
        return (S)manager.getExecutionSettingsProvider().apply(Pair.create(project, linkedProjectPath));
    }

    @Contract("null -> false, _")
    public static String getExtensionSystemOption(@Nullable Module module, String key) {
        if (module == null) {
            return null;
        }
        ExternalSystemModuleExtension extension = module.getExtension(ExternalSystemModuleExtension.class);
        if (extension == null) {
            return null;
        }
        return extension.getOption(key);
    }

    @Contract("_, null -> false")
    public static boolean isExternalSystemAwareModule(ProjectSystemId systemId, @Nullable Module module) {
        String extensionSystemOption = getExtensionSystemOption(module, ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY);
        return extensionSystemOption != null && systemId.getId().equals(extensionSystemOption);
    }

    @Contract("_, null -> false")
    public static boolean isExternalSystemAwareModule(String systemId, @Nullable Module module) {
        return module != null && systemId.equals(getExtensionSystemOption(module, ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY));
    }

    public static @Nullable String getExternalProjectPath(@Nullable Module module) {
        return getExtensionSystemOption(module, ExternalSystemConstants.LINKED_PROJECT_PATH_KEY);
    }

    public static @Nullable String getExternalProjectId(@Nullable Module module) {
        return getExtensionSystemOption(module, ExternalSystemConstants.LINKED_PROJECT_ID_KEY);
    }

    @Nullable
    @RequiredUIAccess
    public static VirtualFile findLocalFileByPath(String path) {
        VirtualFile result = StandardFileSystems.local().findFileByPath(path);
        if (result != null) {
            return result;
        }

        return !Application.get().isReadAccessAllowed()
            ? findLocalFileByPathUnderWriteAction(path)
            : findLocalFileByPathUnderReadAction(path);
    }

    @Nullable
    @RequiredUIAccess
    private static VirtualFile findLocalFileByPathUnderWriteAction(String path) {
        return ExternalSystemApiUtil.doWriteAction(() -> StandardFileSystems.local().refreshAndFindFileByPath(path));
    }

    private static @Nullable VirtualFile findLocalFileByPathUnderReadAction(String path) {
        return AccessRule.read(() -> StandardFileSystems.local().findFileByPath(path));
    }

    private static Throwable unwrapException(Throwable e) {
        for (Throwable candidate = e; candidate != null; candidate = candidate.getCause()) {
            Class<? extends Throwable> clazz = candidate.getClass();
            if (clazz != InvocationTargetException.class && clazz != UndeclaredThrowableException.class) {
                return candidate;
            }
        }
        return e;
    }
}
