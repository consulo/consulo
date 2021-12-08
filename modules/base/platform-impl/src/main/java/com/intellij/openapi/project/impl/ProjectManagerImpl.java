/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.project.impl;

import com.intellij.conversion.ConversionResult;
import com.intellij.conversion.ConversionService;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.impl.ModuleManagerComponent;
import com.intellij.openapi.module.impl.ModuleManagerImpl;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.ex.VirtualFileManagerAdapter;
import com.intellij.openapi.vfs.impl.ZipHandler;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.GuiUtils;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import consulo.annotation.access.RequiredWriteAction;
import consulo.awt.TargetAWT;
import consulo.components.impl.stores.ProjectStorageUtil;
import consulo.components.impl.stores.StorageUtil;
import consulo.components.impl.stores.storage.StateStorageBase;
import consulo.components.impl.stores.storage.StateStorageManager;
import consulo.components.impl.stores.storage.VfsFileBasedStorage;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.start.WelcomeFrameManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Singleton
public class ProjectManagerImpl extends ProjectManagerEx implements Disposable {
  private static final Logger LOG = Logger.getInstance(ProjectManagerImpl.class);

  private static final Key<List<ProjectManagerListener>> LISTENERS_IN_PROJECT_KEY = Key.create("LISTENERS_IN_PROJECT_KEY");

  private Project[] myOpenProjects = {}; // guarded by lock
  private final Object lock = new Object();

  private final List<Predicate<Project>> myCloseProjectVetos = ContainerUtil.createLockFreeCopyOnWriteList();

  private final MultiMap<Project, StateStorage> myChangedProjectFiles = MultiMap.createSet();
  private final SingleAlarm myChangedFilesAlarm;
  private final AtomicInteger myReloadBlockCount = new AtomicInteger(0);

  @Nonnull
  private final Application myApplication;
  private final ProgressManager myProgressManager;

  private final EventDispatcher<ProjectManagerListener> myDeprecatedListenerDispatcher = EventDispatcher.create(ProjectManagerListener.class);

  private final Runnable restartApplicationOrReloadProjectTask = () -> {
    if (isReloadUnblocked()) {
      askToReloadProjectIfConfigFilesChangedExternally();
    }
  };

  @Nonnull
  private static List<ProjectManagerListener> getListeners(Project project) {
    List<ProjectManagerListener> array = project.getUserData(LISTENERS_IN_PROJECT_KEY);
    if (array == null) return Collections.emptyList();
    return array;
  }

  private ExcludeRootsCache myExcludeRootsCache;

  @Inject
  public ProjectManagerImpl(@Nonnull Application application, @Nonnull VirtualFileManager virtualFileManager, ProgressManager progressManager) {
    myApplication = application;
    myProgressManager = progressManager;

    MessageBus messageBus = application.getMessageBus();

    MessageBusConnection connection = messageBus.connect();
    myExcludeRootsCache = new ExcludeRootsCache(connection);
    connection.subscribe(TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        project.getMessageBus().connect(project).subscribe(StateStorage.STORAGE_TOPIC, (event, storage) -> projectStorageFileChanged(event, storage, project));

        myDeprecatedListenerDispatcher.getMulticaster().projectOpened(project, uiAccess);

        for (ProjectManagerListener listener : getListeners(project)) {
          listener.projectOpened(project, uiAccess);
        }
      }

      @Override
      public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        myDeprecatedListenerDispatcher.getMulticaster().projectClosed(project, uiAccess);

        for (ProjectManagerListener listener : getListeners(project)) {
          listener.projectClosed(project, uiAccess);
        }

        ZipHandler.clearFileAccessorCache();
        LaterInvocator.purgeExpiredItems();
      }

      @Override
      public void projectClosing(@Nonnull Project project) {
        myDeprecatedListenerDispatcher.getMulticaster().projectClosing(project);

        for (ProjectManagerListener listener : getListeners(project)) {
          listener.projectClosing(project);
        }
      }
    });
    virtualFileManager.addVirtualFileManagerListener(new VirtualFileManagerAdapter() {
      @Override
      public void beforeRefreshStart(boolean asynchronous) {
        blockReloadingProjectOnExternalChanges();
      }

      @Override
      public void afterRefreshFinish(boolean asynchronous) {
        unblockReloadingProjectOnExternalChanges();
      }
    });
    myChangedFilesAlarm = new SingleAlarm(restartApplicationOrReloadProjectTask, 300);
  }

  private void projectStorageFileChanged(@Nonnull VirtualFileEvent event, @Nonnull StateStorage storage, @Nonnull Project project) {
    VirtualFile file = event.getFile();
    if (!StorageUtil.isChangedByStorageOrSaveSession(event) && !(event.getRequestor() instanceof ProjectManagerImpl)) {
      registerProjectToReload(project, file, storage);
    }
  }

  @Override
  @RequiredWriteAction
  public void dispose() {
    myApplication.assertWriteAccessAllowed();
    Disposer.dispose(myChangedFilesAlarm);
  }

  private static final boolean LOG_PROJECT_LEAKAGE_IN_TESTS = Boolean.getBoolean("LOG_PROJECT_LEAKAGE_IN_TESTS");
  private static final int MAX_LEAKY_PROJECTS = 42;
  @SuppressWarnings("FieldCanBeLocal")
  private final Map<Project, String> myProjects = new WeakHashMap<>();

  @Override
  @Nullable
  public Project newProject(final String projectName, @Nonnull String dirPath, boolean useDefaultProjectSettings, boolean isDummy) {
    dirPath = toCanonicalName(dirPath);

    //noinspection ConstantConditions
    if (LOG_PROJECT_LEAKAGE_IN_TESTS && myApplication.isUnitTestMode()) {
      for (int i = 0; i < 42; i++) {
        if (myProjects.size() < MAX_LEAKY_PROJECTS) break;
        System.gc();
        TimeoutUtil.sleep(100);
        System.gc();
      }

      if (myProjects.size() >= MAX_LEAKY_PROJECTS) {
        List<Project> copy = new ArrayList<>(myProjects.keySet());
        myProjects.clear();
        throw new TooManyProjectLeakedException(copy);
      }
    }

    ProjectImpl project = createProject(projectName, dirPath, false, myApplication.isUnitTestMode());
    try {
      initProject(project, useDefaultProjectSettings ? (ProjectImpl)getDefaultProject() : null);
      if (LOG_PROJECT_LEAKAGE_IN_TESTS) {
        myProjects.put(project, null);
      }
      return project;
    }
    catch (Throwable t) {
      LOG.info(t);
      Messages.showErrorDialog(message(t), ProjectBundle.message("project.load.default.error"));
      return null;
    }
  }

  @Nonnull
  private static String message(Throwable e) {
    String message = e.getMessage();
    if (message != null) return message;
    message = e.getLocalizedMessage();
    //noinspection ConstantConditions
    if (message != null) return message;
    message = e.toString();
    Throwable cause = e.getCause();
    if (cause != null) {
      String causeMessage = message(cause);
      return message + " (cause: " + causeMessage + ")";
    }

    return message;
  }

  private void initProject(@Nonnull final ProjectImpl project, @Nullable ProjectImpl template) throws IOException {
    ProgressIndicator indicator = myProgressManager.getProgressIndicator();
    if (indicator != null && !project.isDefault()) {
      indicator.setText(ProjectBundle.message("loading.components.for", project.getName()));
      indicator.setIndeterminate(true);
    }

    boolean succeed = false;
    try {
      if (template != null) {
        project.getStateStore().loadProjectFromTemplate(template);
      }
      else {
        project.getStateStore().load();
      }
      project.initNotLazyServices(null);

      ModuleManagerImpl moduleManager = ModuleManagerImpl.getInstanceImpl(project);
      moduleManager.setReady(true);

      succeed = true;
    }
    finally {
      if (!succeed && !project.isDefault()) {
        TransactionGuard.submitTransaction(project, () -> WriteAction.run(() -> Disposer.dispose(project)));
      }
    }
  }

  @Nonnull
  private ProjectImpl createProject(@Nullable String projectName, @Nonnull String dirPath, boolean isDefault, boolean isOptimiseTestLoadSpeed) {
    return createProject(projectName, dirPath, isDefault, isOptimiseTestLoadSpeed, false);
  }

  @Nonnull
  private ProjectImpl createProject(@Nullable String projectName, @Nonnull String dirPath, boolean isDefault, boolean isOptimiseTestLoadSpeed, boolean noUICall) {
    return new ProjectImpl(myApplication, this, new File(dirPath).getAbsolutePath(), isOptimiseTestLoadSpeed, projectName, noUICall);
  }

  @Nonnull
  private static String toCanonicalName(@Nonnull final String filePath) {
    try {
      return FileUtil.resolveShortWindowsName(filePath);
    }
    catch (IOException e) {
      // OK. File does not yet exist so it's canonical path will be equal to its original path.
    }

    return filePath;
  }

  @Override
  @Nonnull
  public Project[] getOpenProjects() {
    synchronized (lock) {
      return myOpenProjects;
    }
  }

  @Override
  public boolean isProjectOpened(Project project) {
    synchronized (lock) {
      return ArrayUtil.contains(project, myOpenProjects);
    }
  }

  @Override
  @RequiredUIAccess
  public boolean openProject(@Nonnull final Project project, @Nonnull UIAccess uiAccess) {
    if (isLight(project)) {
      ((ProjectImpl)project).setTemporarilyDisposed(false);
      boolean isInitialized = StartupManagerEx.getInstanceEx(project).startupActivityPassed();
      if (isInitialized) {
        addToOpened(project);
        // events already fired
        return true;
      }
    }

    for (Project p : getOpenProjects()) {
      if (ProjectUtil.isSameProject(project.getProjectFilePath(), p)) {
        GuiUtils.invokeLaterIfNeeded(() -> ProjectUtil.focusProjectWindow(p, false), ModalityState.NON_MODAL);
        return false;
      }
    }

    if (!addToOpened(project)) {
      return false;
    }

    Runnable process = () -> {
      TransactionGuard.getInstance().submitTransactionAndWait(() -> myApplication.getMessageBus().syncPublisher(TOPIC).projectOpened(project, uiAccess));

      final StartupManagerImpl startupManager = (StartupManagerImpl)StartupManager.getInstance(project);
      startupManager.runStartupActivities(uiAccess);
      startupManager.runPostStartupActivitiesFromExtensions(uiAccess);

      GuiUtils.invokeLaterIfNeeded(() -> {
        if (!project.isDisposed()) {
          startupManager.runPostStartupActivities(uiAccess);


          if (!myApplication.isHeadlessEnvironment() && !myApplication.isUnitTestMode()) {
            final TrackingPathMacroSubstitutor macroSubstitutor = ((ProjectEx)project).getStateStore().getStateStorageManager().getMacroSubstitutor();
            if (macroSubstitutor != null) {
              StorageUtil.notifyUnknownMacros(macroSubstitutor, project, null);
            }
          }

          if (myApplication.isActive()) {
            Window projectFrame = TargetAWT.to(WindowManager.getInstance().getWindow(project));
            if (projectFrame != null) {
              IdeFocusManager.getInstance(project).requestFocus(projectFrame, true);
            }
          }
        }
      }, ModalityState.NON_MODAL);
    };

    ProgressIndicator indicator = myProgressManager.getProgressIndicator();
    if (indicator != null) {
      indicator.setText("Preparing workspace...");
      process.run();
      return true;
    }

    boolean ok = myProgressManager.runProcessWithProgressSynchronously(process, "Preparing workspace...", canCancelProjectLoading(), project);
    if (!ok) {
      closeProject(project, false, false, true);
      notifyProjectOpenFailed();
      return false;
    }

    return true;
  }

  private boolean addToOpened(@Nonnull Project project) {
    assert !project.isDisposed() : "Must not open already disposed project";
    synchronized (lock) {
      if (isProjectOpened(project)) {
        return false;
      }
      myOpenProjects = ArrayUtil.append(myOpenProjects, project);
      ProjectCoreUtil.theProject = myOpenProjects.length == 1 ? project : null;
    }
    return true;
  }

  @Nonnull
  private Collection<Project> removeFromOpened(@Nonnull Project project) {
    synchronized (lock) {
      myOpenProjects = ArrayUtil.remove(myOpenProjects, project);
      ProjectCoreUtil.theProject = myOpenProjects.length == 1 ? myOpenProjects[0] : null;
      return Arrays.asList(myOpenProjects);
    }
  }

  private static boolean canCancelProjectLoading() {
    ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
    return !(indicator instanceof NonCancelableSection);
  }

  private void notifyProjectOpenFailed() {
    myApplication.getMessageBus().syncPublisher(AppLifecycleListener.TOPIC).projectOpenFailed();

    WelcomeFrameManager.getInstance().showIfNoProjectOpened();
  }

  private void askToReloadProjectIfConfigFilesChangedExternally() {
    Set<Project> projects;
    synchronized (myChangedProjectFiles) {
      if (myChangedProjectFiles.isEmpty()) {
        return;
      }
      projects = new HashSet<>(myChangedProjectFiles.keySet());
    }

    List<Project> projectsToReload = new SmartList<>();
    for (Project project : projects) {
      if (shouldReloadProject(project)) {
        projectsToReload.add(project);
      }
    }

    UIAccess uiAccess = myApplication.getLastUIAccess();

    for (Project project : projectsToReload) {
      doReloadProjectAsync(project, uiAccess);
    }
  }

  private boolean shouldReloadProject(@Nonnull Project project) {
    if (project.isDisposed()) {
      return false;
    }

    Collection<StateStorage> causes = new SmartList<>();
    Collection<StateStorage> changes;
    synchronized (myChangedProjectFiles) {
      changes = myChangedProjectFiles.remove(project);
      if (!ContainerUtil.isEmpty(changes)) {
        for (StateStorage change : changes) {
          causes.add(change);
        }
      }
    }
    return !causes.isEmpty() && askToRestart(causes);
  }

  private static boolean askToRestart(@Nullable Collection<? extends StateStorage> changedStorages) {
    StringBuilder message = new StringBuilder();
    message.append("Project components were changed externally and cannot be reloaded");

    message.append("\nWould you like to ");
    message.append("reload project?");

    if (Messages.showYesNoDialog(message.toString(), "Project Files Changed", Messages.getQuestionIcon()) == Messages.YES) {
      if (changedStorages != null) {
        for (StateStorage storage : changedStorages) {
          if (storage instanceof StateStorageBase) {
            ((StateStorageBase)storage).disableSaving();
          }
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public void blockReloadingProjectOnExternalChanges() {
    myReloadBlockCount.incrementAndGet();
  }

  @Override
  public void unblockReloadingProjectOnExternalChanges() {
    if (myReloadBlockCount.decrementAndGet() == 0 && myChangedFilesAlarm.isEmpty()) {
      myApplication.invokeLater(restartApplicationOrReloadProjectTask, ModalityState.NON_MODAL);
    }
  }

  private boolean isReloadUnblocked() {
    int count = myReloadBlockCount.get();
    if (LOG.isDebugEnabled()) {
      LOG.debug("[RELOAD] myReloadBlockCount = " + count);
    }
    return count == 0;
  }

  @Override
  @RequiredUIAccess
  public void openTestProject(@Nonnull final Project project) {
    assert myApplication.isUnitTestMode();
    openProject(project);
    UIUtil.dispatchAllInvocationEvents(); // post init activities are invokeLatered
  }

  @Override
  @RequiredUIAccess
  public Collection<Project> closeTestProject(@Nonnull Project project) {
    assert myApplication.isUnitTestMode();
    closeProject(project, false, false, false);
    Project[] projects = getOpenProjects();
    return projects.length == 0 ? Collections.<Project>emptyList() : Arrays.asList(projects);
  }

  @Override
  public void saveChangedProjectFile(@Nonnull VirtualFile file, @Nonnull Project project) {
    StateStorageManager storageManager = ((ProjectEx)project).getStateStore().getStateStorageManager();
    String fileSpec = storageManager.collapseMacros(file.getPath());
    Couple<Collection<VfsFileBasedStorage>> storages = storageManager.getCachedFileStateStorages(Collections.singletonList(fileSpec), Collections.<String>emptyList());
    VfsFileBasedStorage storage = ContainerUtil.getFirstItem(storages.first);
    // if empty, so, storage is not yet loaded, so, we don't have to reload
    if (storage != null) {
      registerProjectToReload(project, file, storage);
    }
  }

  private void registerProjectToReload(@Nonnull Project project, @Nonnull VirtualFile file, @Nonnull StateStorage storage) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("[RELOAD] Registering project to reload: " + file, new Exception());
    }

    myChangedProjectFiles.putValue(project, storage);

    if (storage instanceof StateStorageBase) {
      ((StateStorageBase)storage).disableSaving();
    }

    if (isReloadUnblocked()) {
      myChangedFilesAlarm.cancelAndRequest();
    }
  }

  @Override
  public void reloadProject(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    myChangedProjectFiles.remove(project);
    doReloadProjectAsync(project, uiAccess);
  }

  private void doReloadProjectAsync(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    ProjectReloadState.getInstance(project).onBeforeAutomaticProjectReload();

    if(project.isDisposed()) {
      return;
    }

    String basePath = project.getBasePath();

    closeAndDisposeAsync(project, uiAccess).doWhenDone(() -> ProjectUtil.openAsync(basePath, null, true, uiAccess));
  }

  @Override
  @RequiredUIAccess
  public boolean closeProject(@Nonnull final Project project) {
    return closeProject(project, true, false, true);
  }

  @RequiredUIAccess
  public boolean closeProject(@Nonnull final Project project, final boolean save, final boolean dispose, boolean checkCanClose) {
    if (isLight(project)) {
      removeFromOpened(project);
      return true;
    }
    else {
      if (!isProjectOpened(project)) return true;
    }

    if (checkCanClose && !canClose(project)) return false;
    final ShutDownTracker shutDownTracker = ShutDownTracker.getInstance();
    shutDownTracker.registerStopperThread(Thread.currentThread());
    try {
      if (save) {
        FileDocumentManager.getInstance().saveAllDocuments();
        project.save();
      }

      if (checkCanClose && !ensureCouldCloseIfUnableToSave(project)) {
        return false;
      }

      myApplication.getMessageBus().syncPublisher(TOPIC).projectClosing(project); // somebody can start progress here, do not wrap in write action

      UIAccess uiAccess = UIAccess.current();

      myApplication.runWriteAction(() -> {
        removeFromOpened(project);

        myApplication.getMessageBus().syncPublisher(TOPIC).projectClosed(project, uiAccess);

        if (dispose) {
          Disposer.dispose(project);
        }
      });
    }
    finally {
      shutDownTracker.unregisterStopperThread(Thread.currentThread());
    }

    return true;
  }

  public boolean isLight(@Nonnull Project project) {
    return myApplication.isUnitTestMode() && project.toString().contains("light_temp_");
  }

  @RequiredUIAccess
  @Override
  public boolean closeAndDispose(@Nonnull final Project project) {
    return closeProject(project, true, true, true);
  }

  @Override
  public void addProjectManagerListener(@Nonnull ProjectManagerListener listener) {
    myDeprecatedListenerDispatcher.addListener(listener);
  }

  @Override
  public void addProjectManagerListener(@Nonnull final ProjectManagerListener listener, @Nonnull Disposable parentDisposable) {
    myDeprecatedListenerDispatcher.addListener(listener, parentDisposable);
  }

  @Override
  public void removeProjectManagerListener(@Nonnull ProjectManagerListener listener) {
    myDeprecatedListenerDispatcher.removeListener(listener);
  }

  @Override
  public void addProjectManagerListener(@Nonnull Project project, @Nonnull ProjectManagerListener listener) {
    List<ProjectManagerListener> listeners = project.getUserData(LISTENERS_IN_PROJECT_KEY);
    if (listeners == null) {
      listeners = ((UserDataHolderEx)project).putUserDataIfAbsent(LISTENERS_IN_PROJECT_KEY, ContainerUtil.<ProjectManagerListener>createLockFreeCopyOnWriteList());
    }
    listeners.add(listener);
  }

  @Override
  public void removeProjectManagerListener(@Nonnull Project project, @Nonnull ProjectManagerListener listener) {
    List<ProjectManagerListener> listeners = project.getUserData(LISTENERS_IN_PROJECT_KEY);
    LOG.assertTrue(listeners != null);
    boolean removed = listeners.remove(listener);
    LOG.assertTrue(removed);
  }

  @Override
  public boolean canClose(Project project) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: canClose()");
    }

    for (Predicate<Project> listener : myCloseProjectVetos) {
      try {
        if (!listener.test(project)) return false;
      }
      catch (Throwable e) {
        LOG.warn(e); // DO NOT LET ANY PLUGIN to prevent closing due to exception
      }
    }

    return true;
  }

  @Nonnull
  @Override
  public Disposable registerCloseProjectVeto(@Nonnull Predicate<Project> projectVeto) {
    myCloseProjectVetos.add(projectVeto);
    return () -> myCloseProjectVetos.remove(projectVeto);
  }

  private static boolean ensureCouldCloseIfUnableToSave(@Nonnull final Project project) {
    final ProjectStorageUtil.UnableToSaveProjectNotification[] notifications =
            NotificationsManager.getNotificationsManager().getNotificationsOfType(ProjectStorageUtil.UnableToSaveProjectNotification.class, project);
    if (notifications.length == 0) return true;

    final String fileNames = StringUtil.join(notifications[0].getFileNames(), "\n");

    final String msg = String.format("%s was unable to save some project files,\nare you sure you want to close this project anyway?", ApplicationNamesInfo.getInstance().getProductName());
    return Messages.showDialog(project, msg, "Unsaved Project", "Read-only files:\n\n" + fileNames, new String[]{"Yes", "No"}, 0, 1, Messages.getWarningIcon()) == 0;
  }

  @Nonnull
  @Override
  public AsyncResult<Project> openProjectAsync(@Nonnull VirtualFile file, @Nonnull UIAccess uiAccess) {
    AsyncResult<Project> projectAsyncResult = AsyncResult.undefined();

    AsyncResult<ConversionResult> preparingResult = AsyncResult.undefined();
    String fp = toCanonicalName(file.getPath());

    preparingResult.doWhenRejected(projectAsyncResult::reject);
    preparingResult.doWhenDone(conversionResult -> tryInitProjectByPath(conversionResult, projectAsyncResult, file, uiAccess));

    Task.Backgroundable.queue(null, "Preparing project...", canCancelProjectLoading(), (indicator) -> {
      final ConversionResult conversionResult = ConversionService.getInstance().convert(fp);
      if (conversionResult.openingIsCanceled()) {
        preparingResult.reject("conversion canceled");
        return;
      }
      preparingResult.setDone(conversionResult);
    });
    return projectAsyncResult;
  }

  @Nonnull
  @Override
  public AsyncResult<Project> openProjectAsync(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    AsyncResult<Project> projectAsyncResult = AsyncResult.undefined();
    loadProjectAsync((ProjectImpl)project, projectAsyncResult, false, ConversionResult.DUMMY, uiAccess);
    return projectAsyncResult;
  }

  @Nonnull
  @Override
  public String[] getAllExcludedUrls() {
    return myExcludeRootsCache.getExcludedUrls();
  }

  @Nonnull
  @Override
  public AsyncResult<Void> closeAndDisposeAsync(@Nonnull Project project, @Nonnull UIAccess uiAccess, boolean checkCanClose, boolean save, boolean dispose) {
    if (isLight(project)) {
      removeFromOpened(project);
      return AsyncResult.resolved();
    }
    else {
      if (!isProjectOpened(project)) {
        return AsyncResult.resolved();
      }
    }

    AsyncResult<Void> mainResult = AsyncResult.undefined();

    AsyncResult<Void> closeCheckInsideUI = AsyncResult.undefined();

    if (checkCanClose) {
      uiAccess.give(() -> {
        boolean canClose = canClose(project);
        if (canClose) {
          closeCheckInsideUI.setDone();
        }
        else {
          closeCheckInsideUI.setRejected();
        }
      });
    }
    else {
      closeCheckInsideUI.setDone();
    }

    closeCheckInsideUI.doWhenRejected((Runnable)mainResult::setRejected);

    closeCheckInsideUI.doWhenDone(() -> {
      final Thread executeThread = Thread.currentThread();
      final ShutDownTracker shutDownTracker = ShutDownTracker.getInstance();
      shutDownTracker.registerStopperThread(executeThread);
      try {
        if(save) {
          uiAccess.giveAndWaitIfNeed(() -> {
            FileDocumentManager.getInstance().saveAllDocuments();
            project.save();
          });
        }

        myApplication.getMessageBus().syncPublisher(TOPIC).projectClosing(project); // somebody can start progress here, do not wrap in write action

        WriteAction.runAndWait(() -> {
          removeFromOpened(project);

          myApplication.getMessageBus().syncPublisher(TOPIC).projectClosed(project, uiAccess);

          if (dispose) {
            Disposer.dispose(project);
          }
        });

        mainResult.setDone();
      }
      catch (Throwable e) {
        LOG.error(e);
        mainResult.rejectWithThrowable(e);
      }
      finally {
        shutDownTracker.unregisterStopperThread(Thread.currentThread());
      }
    });
    return mainResult;
  }

  private void tryInitProjectByPath(ConversionResult conversionResult, AsyncResult<Project> projectAsyncResult, VirtualFile path, UIAccess uiAccess) {
    final ProjectImpl project = createProject(null, toCanonicalName(path.getPath()), false, false, true);

    for (Project p : getOpenProjects()) {
      if (ProjectUtil.isSameProject(path.getPath(), p)) {
        uiAccess.give(() -> ProjectUtil.focusProjectWindow(p, false));
        closeAndDisposeAsync(project, uiAccess).doWhenProcessed(() -> projectAsyncResult.reject("Already opened project"));
        return;
      }
    }

    loadProjectAsync(project, projectAsyncResult, true, conversionResult, uiAccess);
  }

  private void loadProjectAsync(final ProjectImpl project, AsyncResult<Project> projectAsyncResult, boolean init, ConversionResult conversionResult, UIAccess uiAccess) {
    Task.Backgroundable.queue(project, ProjectBundle.message("project.load.progress"), canCancelProjectLoading(), progressIndicator -> {
      progressIndicator.setIndeterminate(true);

      try {
        if (!addToOpened(project)) {
          closeAndDisposeAsync(project, uiAccess).doWhenProcessed(() -> projectAsyncResult.reject("Can't add project to opened"));
          return;
        }

        if (init) {
          initProjectAsync(project, null, progressIndicator);
        }

        prepareModules(conversionResult,project, uiAccess, projectAsyncResult).doWhenDone(() -> {
          prepareProjectWorkspace(conversionResult, project, uiAccess, projectAsyncResult);
        });
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);

        projectAsyncResult.rejectWithThrowable(e);
      }
    });
  }

  private AsyncResult<Void> prepareModules(ConversionResult conversionResult, Project project, UIAccess uiAccess, AsyncResult<Project> projectAsyncResult) {
    AsyncResult<Void> result = AsyncResult.undefined();

    Task.Backgroundable.queue(project, "Loading modules...", canCancelProjectLoading(), indicator -> {
      ModuleManagerComponent moduleManager = (ModuleManagerComponent)ModuleManager.getInstance(project);

      try {
        moduleManager.loadModules(indicator, result);
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);

        result.rejectWithThrowable(e);
        projectAsyncResult.rejectWithThrowable(e);
      }
    });

    return result;
  }

  private void prepareProjectWorkspace(ConversionResult conversionResult, Project project, UIAccess uiAccess, AsyncResult<Project> projectAsyncResult) {
    Task.Backgroundable.queue(project, "Preparing workspace...", canCancelProjectLoading(), progressIndicator -> {
      progressIndicator.setIndeterminate(true);

      try {
        StartupManager.getInstance(project).registerPostStartupActivity(() -> conversionResult.postStartupActivity(project));

        openProjectRequireBackgroundTask(project, uiAccess);

        projectAsyncResult.setDone(project);
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);

        projectAsyncResult.rejectWithThrowable(e);
      }
    });
  }

  private void openProjectRequireBackgroundTask(Project project, UIAccess uiAccess) {
    myApplication.getMessageBus().syncPublisher(TOPIC).projectOpened(project, uiAccess);

    final StartupManagerImpl startupManager = (StartupManagerImpl)StartupManager.getInstance(project);
    startupManager.runStartupActivities(uiAccess);
    startupManager.runPostStartupActivitiesFromExtensions(uiAccess);

    if (!project.isDisposed()) {
      startupManager.runPostStartupActivities(uiAccess);

      Application application = Application.get();
      if (!application.isHeadlessEnvironment() && !application.isUnitTestMode()) {
        final TrackingPathMacroSubstitutor macroSubstitutor = ((ProjectEx)project).getStateStore().getStateStorageManager().getMacroSubstitutor();
        if (macroSubstitutor != null) {
          StorageUtil.notifyUnknownMacros(macroSubstitutor, project, null);
        }
      }

      if (application.isActive()) {
        consulo.ui.Window projectFrame = WindowManager.getInstance().getWindow(project);
        if (projectFrame != null) {
          uiAccess.giveAndWaitIfNeed(() -> IdeFocusManager.getInstance(project).requestFocus(projectFrame, true));
        }
      }

      application.invokeLater(() -> {
        if (!project.isDisposedOrDisposeInProgress()) {
          startupManager.scheduleBackgroundPostStartupActivities(uiAccess);
        }
      }, ModalityState.NON_MODAL, o -> project.isDisposedOrDisposeInProgress());
    }
  }

  private void initProjectAsync(@Nonnull final ProjectImpl project, @Nullable ProjectImpl template, ProgressIndicator progressIndicator) throws IOException {
    progressIndicator.setText(ProjectBundle.message("loading.components.for", project.getName()));

    boolean succeed = false;
    try {
      if (template != null) {
        project.getStateStore().loadProjectFromTemplate(template);
      }
      else {
        project.getStateStore().load();
      }
      project.initNotLazyServices(progressIndicator);
      succeed = true;
    }
    catch (Throwable e) {
      LOG.error(e);
    }
    finally {
      if (!succeed && !project.isDefault()) {
        TransactionGuard.submitTransaction(project, () -> WriteAction.run(() -> Disposer.dispose(project)));
      }
    }
  }
}
