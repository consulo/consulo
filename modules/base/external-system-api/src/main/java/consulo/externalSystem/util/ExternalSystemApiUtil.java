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

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.TransactionGuard;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.Semaphore;
import consulo.application.util.function.Computable;
import consulo.application.util.registry.Registry;
import consulo.content.OrderRootType;
import consulo.content.library.Library;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.RunnerRegistry;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.event.ExecutionListener;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.externalSystem.ExternalSystemAutoImportAware;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.internal.ui.ExternalSystemRecentTasksList;
import consulo.externalSystem.model.*;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.externalSystem.model.project.LibraryData;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.externalSystem.model.task.TaskCallback;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.service.ParametersEnhancer;
import consulo.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import consulo.externalSystem.service.execution.ExternalSystemRunConfiguration;
import consulo.externalSystem.service.module.extension.ExternalSystemModuleExtension;
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
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.io.ClassPathUtil;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.util.nodep.classloader.UrlClassLoader;
import consulo.util.rmi.RemoteUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.virtualFileSystem.util.PathsList;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Denis Zhdanov
 * @since 4/1/13 1:31 PM
 */
public class ExternalSystemApiUtil {

  private static final Logger LOG = Logger.getInstance(ExternalSystemApiUtil.class);
  private static final String LAST_USED_PROJECT_PATH_PREFIX = "LAST_EXTERNAL_PROJECT_PATH_";

  @Nonnull
  private static final Map<String, String> RUNNER_IDS = new HashMap<>();

  static {
    RUNNER_IDS.put(DefaultRunExecutor.EXECUTOR_ID, ExternalSystemConstants.RUNNER_ID);
    RUNNER_IDS.put(DefaultDebugExecutor.EXECUTOR_ID, ExternalSystemConstants.DEBUG_RUNNER_ID);
  }

  public interface TaskUnderProgress {
    void execute(@Nonnull ProgressIndicator indicator);
  }

  @Nonnull
  public static final String PATH_SEPARATOR = "/";

  @Nonnull
  private static final Pattern ARTIFACT_PATTERN = Pattern.compile("(?:.*/)?(.+?)(?:-([\\d+](?:\\.[\\d]+)*))?(?:\\.[^\\.]+?)?");

  @Nonnull
  public static final Comparator<Object> ORDER_AWARE_COMPARATOR = new Comparator<Object>() {

    @Override
    public int compare(@Nonnull Object o1, @Nonnull Object o2) {
      int order1 = getOrder(o1);
      int order2 = getOrder(o2);
      return (order1 < order2) ? -1 : ((order1 == order2) ? 0 : 1);
    }

    private int getOrder(@Nonnull Object o) {
      Queue<Class<?>> toCheck = new ArrayDeque<Class<?>>();
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

  @Nonnull
  private static final Function<DataNode<?>, Key<?>> GROUPER = new Function<DataNode<?>, Key<?>>() {
    @Override
    public Key<?> apply(DataNode<?> node) {
      return node.getKey();
    }
  };

  @Nonnull
  private static final Comparator<Object> COMPARABLE_GLUE = new Comparator<Object>() {
    @SuppressWarnings("unchecked")
    @Override
    public int compare(Object o1, Object o2) {
      return ((Comparable)o1).compareTo(o2);
    }
  };

  private ExternalSystemApiUtil() {
  }

  @Nullable
  public static String getRunnerId(@Nonnull String executorId) {
    return RUNNER_IDS.get(executorId);
  }

  public static void runTask(@Nonnull ExternalSystemTaskExecutionSettings taskSettings, @Nonnull String executorId, @Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
    runTask(taskSettings, executorId, project, externalSystemId, null, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
  }

  public static void runTask(@Nonnull final ExternalSystemTaskExecutionSettings taskSettings,
                             @Nonnull final String executorId,
                             @Nonnull final Project project,
                             @Nonnull final ProjectSystemId externalSystemId,
                             @Nullable final TaskCallback callback,
                             @Nonnull final ProgressExecutionMode progressExecutionMode) {
    final Pair<ProgramRunner, ExecutionEnvironment> pair = createRunner(taskSettings, executorId, project, externalSystemId);
    if (pair == null) return;

    final ProgramRunner runner = pair.first;
    final ExecutionEnvironment environment = pair.second;

    final TaskUnderProgress task = new TaskUnderProgress() {
      @Override
      public void execute(@Nonnull ProgressIndicator indicator) {
        final Semaphore targetDone = new Semaphore();
        final Ref<Boolean> result = new Ref<Boolean>(false);
        final Disposable disposable = Disposable.newDisposable();

        project.getMessageBus().connect(disposable).subscribe(ExecutionListener.class, new ExecutionListener() {
          @Override
          public void processStartScheduled(final String executorIdLocal, final ExecutionEnvironment environmentLocal) {
            if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
              targetDone.down();
            }
          }

          @Override
          public void processNotStarted(final String executorIdLocal, @Nonnull final ExecutionEnvironment environmentLocal) {
            if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
              targetDone.up();
            }
          }

          @Override
          public void processStarted(final String executorIdLocal, @Nonnull final ExecutionEnvironment environmentLocal, @Nonnull final ProcessHandler handler) {
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
          ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
              try {
                runner.execute(environment);
              }
              catch (ExecutionException e) {
                targetDone.up();
                LOG.error(e);
              }
            }
          }, Application.get().getNoneModalityState());
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
      }
    };

    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        final String title = AbstractExternalSystemTaskConfigurationType.generateName(project, taskSettings);
        switch (progressExecutionMode) {
          case MODAL_SYNC:
            new Task.Modal(project, title, true) {
              @Override
              public void run(@Nonnull ProgressIndicator indicator) {
                task.execute(indicator);
              }
            }.queue();
            break;
          case IN_BACKGROUND_ASYNC:
            new Task.Backgroundable(project, title) {
              @Override
              public void run(@Nonnull ProgressIndicator indicator) {
                task.execute(indicator);
              }
            }.queue();
            break;
          case START_IN_FOREGROUND_ASYNC:
            new Task.Backgroundable(project, title, true, PerformInBackgroundOption.DEAF) {
              @Override
              public void run(@Nonnull ProgressIndicator indicator) {
                task.execute(indicator);
              }
            }.queue();
        }
      }
    });
  }


  @Nullable
  public static AbstractExternalSystemTaskConfigurationType findConfigurationType(@Nonnull ProjectSystemId externalSystemId) {
    for (ConfigurationType type : ConfigurationType.EP_NAME.getExtensionList()) {
      if (type instanceof AbstractExternalSystemTaskConfigurationType) {
        AbstractExternalSystemTaskConfigurationType candidate = (AbstractExternalSystemTaskConfigurationType)type;
        if (externalSystemId.equals(candidate.getExternalSystemId())) {
          return candidate;
        }
      }
    }
    return null;
  }

  @Nullable
  public static Pair<ProgramRunner, ExecutionEnvironment> createRunner(@Nonnull ExternalSystemTaskExecutionSettings taskSettings,
                                                                       @Nonnull String executorId,
                                                                       @Nonnull Project project,
                                                                       @Nonnull ProjectSystemId externalSystemId) {
    Executor executor = ExecutorRegistry.getInstance().getExecutorById(executorId);
    if (executor == null) return null;

    String runnerId = getRunnerId(executorId);
    if (runnerId == null) return null;

    ProgramRunner runner = RunnerRegistry.getInstance().findRunnerById(runnerId);
    if (runner == null) return null;

    AbstractExternalSystemTaskConfigurationType configurationType = findConfigurationType(externalSystemId);
    if (configurationType == null) return null;

    String name = AbstractExternalSystemTaskConfigurationType.generateName(project, taskSettings);
    RunnerAndConfigurationSettings settings = RunManager.getInstance(project).createRunConfiguration(name, configurationType.getFactory());
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
  public static void updateRecentTasks(@Nonnull ExternalTaskExecutionInfo taskInfo, @Nonnull Project project) {
    ProjectSystemId externalSystemId = taskInfo.getSettings().getExternalSystemId();
    ExternalSystemRecentTasksList recentTasksList = getToolWindowElement(ExternalSystemRecentTasksList.class, project, ExternalSystemDataKeys.RECENT_TASKS_LIST, externalSystemId);
    if (recentTasksList == null) {
      return;
    }
    recentTasksList.setFirst(taskInfo);

    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;
    AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().apply(project);
    settings.setRecentTasks(recentTasksList.getModel().getTasks());
  }


  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T getToolWindowElement(@Nonnull Class<T> clazz, @Nonnull Project project, @Nonnull consulo.util.dataholder.Key<T> key, @Nonnull ProjectSystemId externalSystemId) {
    if (project.isDisposed() || !project.isOpen()) {
      return null;
    }
    final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    if (toolWindowManager == null) {
      return null;
    }
    final ToolWindow toolWindow = ensureToolWindowContentInitialized(project, externalSystemId);
    if (toolWindow == null) {
      return null;
    }

    final ContentManager contentManager = toolWindow.getContentManager();

    for (Content content : contentManager.getContents()) {
      final JComponent component = content.getComponent();
      if (component instanceof DataProvider) {
        final Object data = ((DataProvider)component).getData(key);
        if (data != null && clazz.isInstance(data)) {
          return (T)data;
        }
      }
    }
    return null;
  }


  @Nullable
  public static ToolWindow ensureToolWindowContentInitialized(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
    final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    if (toolWindowManager == null) return null;

    final ToolWindow toolWindow = toolWindowManager.getToolWindow(externalSystemId.getReadableName());
    if (toolWindow == null) return null;

    // call content manager - initialize it
    toolWindow.getContentManager();
    return toolWindow;
  }

  @Nonnull
  public static String extractNameFromPath(@Nonnull String path) {
    String strippedPath = stripPath(path);
    final int i = strippedPath.lastIndexOf(PATH_SEPARATOR);
    final String result;
    if (i < 0 || i >= strippedPath.length() - 1) {
      result = strippedPath;
    }
    else {
      result = strippedPath.substring(i + 1);
    }
    return result;
  }

  @Nonnull
  private static String stripPath(@Nonnull String path) {
    String[] endingsToStrip = {"/", "!", ".jar"};
    StringBuilder buffer = new StringBuilder(path);
    for (String ending : endingsToStrip) {
      if (buffer.lastIndexOf(ending) == buffer.length() - ending.length()) {
        buffer.setLength(buffer.length() - ending.length());
      }
    }
    return buffer.toString();
  }

  @Nonnull
  public static String getLibraryName(@Nonnull Library library) {
    final String result = library.getName();
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

  public static boolean isRelated(@Nonnull Library library, @Nonnull LibraryData libraryData) {
    return getLibraryName(library).equals(libraryData.getInternalName());
  }

  public static boolean isExternalSystemLibrary(@Nonnull Library library, @Nonnull ProjectSystemId externalSystemId) {
    return library.getName() != null && StringUtil.startsWith(library.getName(), externalSystemId.getReadableName() + ": ");
  }

  @Nullable
  public static ArtifactInfo parseArtifactInfo(@Nonnull String fileName) {
    Matcher matcher = ARTIFACT_PATTERN.matcher(fileName);
    if (!matcher.matches()) {
      return null;
    }
    return new ArtifactInfo(matcher.group(1), null, matcher.group(2));
  }

  @Deprecated
  public static void orderAwareSort(@Nonnull List<?> data) {
    Collections.sort(data, ORDER_AWARE_COMPARATOR);
  }

  /**
   * @param path target path
   * @return absolute path that points to the same location as the given one and that uses only slashes
   */
  @Nonnull
  public static String toCanonicalPath(@Nonnull String path) {
    String p = normalizePath(new File(path).getAbsolutePath());
    assert p != null;
    return PathUtil.getCanonicalPath(p);
  }

  @Nonnull
  public static String getLocalFileSystemPath(@Nonnull VirtualFile file) {
    final VirtualFile archiveRoot = ArchiveVfsUtil.getVirtualFileForArchive(file);
    if (archiveRoot != null) {
      return archiveRoot.getPath();
    }
    return toCanonicalPath(file.getPath());
  }

  @Nullable
  public static ExternalSystemManager<?, ?, ?, ?, ?> getManager(@Nonnull ProjectSystemId externalSystemId) {
    for (ExternalSystemManager manager : ExternalSystemManager.EP_NAME.getExtensionList()) {
      if (externalSystemId.equals(manager.getSystemId())) {
        return manager;
      }
    }
    return null;
  }

  @SuppressWarnings("ManualArrayToCollectionCopy")
  @Nonnull
  public static Collection<ExternalSystemManager<?, ?, ?, ?, ?>> getAllManagers() {
    List<ExternalSystemManager<?, ?, ?, ?, ?>> result = new ArrayList<>();
    for (ExternalSystemManager manager : ExternalSystemManager.EP_NAME.getExtensionList()) {
      result.add(manager);
    }
    return result;
  }

  @Nonnull
  public static Map<Key<?>, List<DataNode<?>>> group(@Nonnull Collection<DataNode<?>> nodes) {
    return groupBy(nodes, GROUPER);
  }

  @Nonnull
  public static <K, V> Map<DataNode<K>, List<DataNode<V>>> groupBy(@Nonnull Collection<DataNode<V>> nodes, @Nonnull final Key<K> key) {
    return groupBy(nodes, new Function<DataNode<V>, DataNode<K>>() {
      @Nullable
      @Override
      public DataNode<K> apply(DataNode<V> node) {
        return node.getDataNode(key);
      }
    });
  }

  @Nonnull
  public static <K, V> Map<K, List<V>> groupBy(@Nonnull Collection<V> nodes, @Nonnull Function<V, K> grouper) {
    Map<K, List<V>> result = new HashMap<K, List<V>>();
    for (V data : nodes) {
      K key = grouper.apply(data);
      if (key == null) {
        LOG.warn(String.format("Skipping entry '%s' during grouping. Reason: it's not possible to build a grouping key with grouping strategy '%s'. " + "Given entries: %s", data, grouper.getClass(),
                               nodes));
        continue;
      }
      List<V> grouped = result.get(key);
      if (grouped == null) {
        result.put(key, grouped = new ArrayList<>());
      }
      grouped.add(data);
    }

    if (!result.isEmpty() && result.keySet().iterator().next() instanceof Comparable) {
      List<K> ordered = new ArrayList<K>(result.keySet());
      Collections.sort(ordered, COMPARABLE_GLUE);
      Map<K, List<V>> orderedResult = new LinkedHashMap<K, List<V>>();
      for (K k : ordered) {
        orderedResult.put(k, result.get(k));
      }
      return orderedResult;
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  public static <T> Collection<DataNode<T>> getChildren(@Nonnull DataNode<?> node, @Nonnull Key<T> key) {
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
  @Nullable
  public static <T> DataNode<T> find(@Nonnull DataNode<?> node, @Nonnull Key<T> key) {
    for (DataNode<?> child : node.getChildren()) {
      if (key.equals(child.getKey())) {
        return (DataNode<T>)child;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> DataNode<T> find(@Nonnull DataNode<?> node, @Nonnull Key<T> key, Predicate<DataNode<T>> predicate) {
    for (DataNode<?> child : node.getChildren()) {
      if (key.equals(child.getKey()) && predicate.test((DataNode<T>)child)) {
        return (DataNode<T>)child;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> DataNode<T> findParent(@Nonnull DataNode<?> node, @Nonnull Key<T> key) {
    return findParent(node, key, null);
  }


  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> DataNode<T> findParent(@Nonnull DataNode<?> node, @Nonnull Key<T> key, @Nullable Predicate<DataNode<T>> predicate) {
    DataNode<?> parent = node.getParent();
    if (parent == null) return null;
    return key.equals(parent.getKey()) && (predicate == null || predicate.test((DataNode<T>)parent)) ? (DataNode<T>)parent : findParent(parent, key, predicate);
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  public static <T> Collection<DataNode<T>> findAll(@Nonnull DataNode<?> parent, @Nonnull Key<T> key) {
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

  public static void executeProjectChangeAction(@Nonnull final DisposeAwareProjectChange task) {
    executeProjectChangeAction(false, task);
  }

  public static void executeProjectChangeAction(boolean synchronous, @Nonnull final DisposeAwareProjectChange task) {
    TransactionGuard.getInstance().assertWriteSafeContext(Application.get().getDefaultModalityState());
    executeOnEdt(synchronous, () -> ApplicationManager.getApplication().runWriteAction(task));
  }

  public static void executeOnEdt(boolean synchronous, @Nonnull Runnable task) {
    final Application app = ApplicationManager.getApplication();
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

  public static <T> T executeOnEdt(@Nonnull final Computable<T> task) {
    final Application app = ApplicationManager.getApplication();
    final Ref<T> result = Ref.create();
    app.invokeAndWait(() -> result.set(task.compute()));
    return result.get();
  }

  public static <T> T doWriteAction(@Nonnull final Computable<T> task) {
    return executeOnEdt(() -> ApplicationManager.getApplication().runWriteAction(task));
  }

  public static void doWriteAction(@Nonnull final Runnable task) {
    executeOnEdt(true, () -> ApplicationManager.getApplication().runWriteAction(task));
  }

  /**
   * Adds runnable to Event Dispatch Queue
   * if we aren't in UnitTest of Headless environment mode
   *
   * @param runnable Runnable
   */
  public static void addToInvokeLater(final Runnable runnable) {
    final Application application = ApplicationManager.getApplication();
    if (application.isHeadlessEnvironment() || application.isDispatchThread()) {
      runnable.run();
    }
    else {
      application.getLastUIAccess().giveIfNeed(runnable);
    }
  }

  /**
   * Configures given classpath to reference target i18n bundle file(s).
   *
   * @param classPath    process classpath
   * @param bundlePath   path to the target bundle file
   * @param contextClass class from the same content root as the target bundle file
   */
  public static void addBundle(@Nonnull PathsList classPath, @Nonnull String bundlePath, @Nonnull Class<?> contextClass) {
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
  @Nullable
  public static String normalizePath(@Nullable String s) {
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

//  @NotNull
//  public static String getLastUsedExternalProjectPath(@NotNull ProjectSystemId externalSystemId) {
//    return PropertiesComponent.getInstance().getValue(LAST_USED_PROJECT_PATH_PREFIX + externalSystemId.getReadableName(), "");
//  }

  public static void storeLastUsedExternalProjectPath(@Nullable String path, @Nonnull ProjectSystemId externalSystemId) {
    if (path != null) {
      ApplicationPropertiesComponent.getInstance().setValue(LAST_USED_PROJECT_PATH_PREFIX + externalSystemId.getReadableName(), path);
    }
  }

  @Nonnull
  public static String getProjectRepresentationName(@Nonnull String targetProjectPath, @Nullable String rootProjectPath) {
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
  @Nullable
  public static String getRootProjectPath(@Nonnull String externalProjectPath, @Nonnull ProjectSystemId externalSystemId, @Nonnull Project project) {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = getManager(externalSystemId);
    if (manager == null) {
      return null;
    }
    if (manager instanceof ExternalSystemAutoImportAware) {
      return ((ExternalSystemAutoImportAware)manager).getAffectedExternalProjectPath(externalProjectPath, project);
    }
    return null;
  }

  /**
   * {@link RemoteUtil#unwrap(Throwable) unwraps} given exception if possible and builds error message for it.
   *
   * @param e exception to process
   * @return error message for the given exception
   */
  @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "IOResourceOpenedButNotSafelyClosed"})
  @Nonnull
  public static String buildErrorMessage(@Nonnull Throwable e) {
    Throwable unwrapped = RemoteUtil.unwrap(e);
    String reason = unwrapped.getLocalizedMessage();
    if (!StringUtil.isEmpty(reason)) {
      return reason;
    }
    else if (unwrapped.getClass() == ExternalSystemException.class) {
      return String.format("exception during working with external system: %s", ((ExternalSystemException)unwrapped).getOriginalReason());
    }
    else {
      StringWriter writer = new StringWriter();
      unwrapped.printStackTrace(new PrintWriter(writer));
      return writer.toString();
    }
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  public static AbstractExternalSystemSettings getSettings(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) throws IllegalArgumentException {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = getManager(externalSystemId);
    if (manager == null) {
      throw new IllegalArgumentException(String.format("Can't retrieve external system settings for id '%s'. Reason: no such external system is registered", externalSystemId.getReadableName()));
    }
    return manager.getSettingsProvider().apply(project);
  }

  @SuppressWarnings("unchecked")
  public static <S extends AbstractExternalSystemLocalSettings> S getLocalSettings(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) throws IllegalArgumentException {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = getManager(externalSystemId);
    if (manager == null) {
      throw new IllegalArgumentException(String.format("Can't retrieve local external system settings for id '%s'. Reason: no such external system is registered", externalSystemId.getReadableName()));
    }
    return (S)manager.getLocalSettingsProvider().apply(project);
  }

  @SuppressWarnings("unchecked")
  public static <S extends ExternalSystemExecutionSettings> S getExecutionSettings(@Nonnull Project project, @Nonnull String linkedProjectPath, @Nonnull ProjectSystemId externalSystemId)
          throws IllegalArgumentException {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = getManager(externalSystemId);
    if (manager == null) {
      throw new IllegalArgumentException(
              String.format("Can't retrieve external system execution settings for id '%s'. Reason: no such external system is registered", externalSystemId.getReadableName()));
    }
    return (S)manager.getExecutionSettingsProvider().apply(Pair.create(project, linkedProjectPath));
  }

  /**
   * Historically we prefer to work with third-party api not from ide process but from dedicated slave process (there is a risk
   * that third-party api has bugs which might make the whole ide process corrupted, e.g. a memory leak at the api might crash
   * the whole ide process).
   * <p>
   * However, we do allow to explicitly configure the ide to work with third-party external system api from the ide process.
   * <p>
   * This method allows to check whether the ide is configured to use 'out of process' or 'in process' mode for the system.
   *
   * @param externalSystemId target external system
   * @return <code>true</code> if the ide is configured to work with external system api from the ide process;
   * <code>false</code> otherwise
   */
  public static boolean isInProcessMode(ProjectSystemId externalSystemId) {
    return Registry.is(externalSystemId.getId() + ExternalSystemConstants.USE_IN_PROCESS_COMMUNICATION_REGISTRY_KEY_SUFFIX, false);
  }

  /**
   * There is a possible case that methods of particular object should be executed with classpath different from the one implied
   * by the current class' class loader. External system offers {@link ParametersEnhancer#enhanceLocalProcessing(List)} method
   * for defining that custom classpath.
   * <p>
   * It's also possible that particular implementation of {@link ParametersEnhancer} is compiled using dependency to classes
   * which are provided by the {@link ParametersEnhancer#enhanceLocalProcessing(List) expanded classpath}. E.g. a class
   * <code>'A'</code> might use method of class <code>'B'</code> and 'A' is located at the current (system/plugin) classpath but
   * <code>'B'</code> is not. We need to reload <code>'A'</code> using its expanded classpath then, i.e. create new class loaded
   * with that expanded classpath and load <code>'A'</code> by it.
   * <p>
   * This method allows to do that.
   *
   * @param clazz custom classpath-aware class which instance should be created (is assumed to have a no-args constructor)
   * @param <T>   target type
   * @return newly created instance of the given class loaded by custom classpath-aware loader
   * @throws IllegalAccessException    as defined by reflection processing
   * @throws InstantiationException    as defined by reflection processing
   * @throws NoSuchMethodException     as defined by reflection processing
   * @throws InvocationTargetException as defined by reflection processing
   * @throws ClassNotFoundException    as defined by reflection processing
   */
  @Nonnull
  public static <T extends ParametersEnhancer> T reloadIfNecessary(@Nonnull final Class<T> clazz)
          throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
    T instance = clazz.newInstance();
    List<URL> urls = new ArrayList<>();
    instance.enhanceLocalProcessing(urls);
    if (urls.isEmpty()) {
      return instance;
    }

    final ClassLoader baseLoader = clazz.getClassLoader();
    Method method = baseLoader.getClass().getMethod("getUrls");
    if (method != null) {
      //noinspection unchecked
      urls.addAll((Collection<? extends URL>)method.invoke(baseLoader));
    }
    UrlClassLoader loader = new UrlClassLoader(UrlClassLoader.build().urls(urls).parent(baseLoader.getParent())) {
      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.equals(clazz.getName())) {
          return super.loadClass(name, resolve);
        }
        else {
          try {
            return baseLoader.loadClass(name);
          }
          catch (ClassNotFoundException e) {
            return super.loadClass(name, resolve);
          }
        }
      }
    };
    //noinspection unchecked
    return (T)loader.loadClass(clazz.getName()).newInstance();
  }

  @Contract("null -> false, _")
  public static String getExtensionSystemOption(@Nullable Module module, @Nonnull String key) {
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
  public static boolean isExternalSystemAwareModule(@Nonnull ProjectSystemId systemId, @Nullable Module module) {
    String extensionSystemOption = getExtensionSystemOption(module, ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY);
    return extensionSystemOption != null && systemId.getId().equals(extensionSystemOption);
  }

  @Contract("_, null -> false")
  public static boolean isExternalSystemAwareModule(@Nonnull String systemId, @Nullable Module module) {
    return module != null && systemId.equals(getExtensionSystemOption(module, ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY));
  }

  @Nullable
  public static String getExternalProjectPath(@Nullable Module module) {
    return getExtensionSystemOption(module, ExternalSystemConstants.LINKED_PROJECT_PATH_KEY);
  }

  @Nullable
  public static String getExternalProjectId(@Nullable Module module) {
    return getExtensionSystemOption(module, ExternalSystemConstants.LINKED_PROJECT_ID_KEY);
  }
}
