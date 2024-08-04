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
package consulo.project.impl.internal;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.internal.NonCancelableSection;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.store.impl.internal.TrackingPathMacroSubstitutor;
import consulo.component.store.impl.internal.storage.StorageUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.logging.Logger;
import consulo.module.ModuleManager;
import consulo.module.impl.internal.ModuleManagerComponent;
import consulo.module.impl.internal.ModuleManagerImpl;
import consulo.project.Project;
import consulo.project.ProjectBundle;
import consulo.project.event.ProjectManagerListener;
import consulo.project.impl.internal.store.IProjectStore;
import consulo.project.internal.ProjectManagerEx;
import consulo.project.internal.ProjectReloadState;
import consulo.project.internal.ProjectWindowFocuser;
import consulo.project.internal.SingleProjectHolder;
import consulo.project.localize.ProjectLocalize;
import consulo.project.startup.StartupManager;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.notification.NotificationsManager;
import consulo.project.ui.wm.WindowManager;
import consulo.project.util.ProjectUtil;
import consulo.proxy.EventDispatcher;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.Lists;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.ShutDownTracker;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Singleton
@ServiceImpl
public class ProjectManagerImpl extends ProjectManagerEx implements Disposable, ProjectManagerImplMarker {
  private static final Logger LOG = Logger.getInstance(ProjectManagerImpl.class);

  private static final Key<List<ProjectManagerListener>> LISTENERS_IN_PROJECT_KEY = Key.create("LISTENERS_IN_PROJECT_KEY");

  private Project[] myOpenProjects = {}; // guarded by lock
  private final Object lock = new Object();

  private final List<Predicate<Project>> myCloseProjectVetos = Lists.newLockFreeCopyOnWriteList();

  @Nonnull
  private final Application myApplication;
  @Nonnull
  private final ComponentBinding myComponentBinding;
  @Nonnull
  private final ProgressIndicatorProvider myProgressManager;

  private final EventDispatcher<ProjectManagerListener> myDeprecatedListenerDispatcher =
    EventDispatcher.create(ProjectManagerListener.class);

  @Nonnull
  private static List<ProjectManagerListener> getListeners(Project project) {
    List<ProjectManagerListener> array = project.getUserData(LISTENERS_IN_PROJECT_KEY);
    if (array == null) return Collections.emptyList();
    return array;
  }

  private ExcludeRootsCache myExcludeRootsCache;

  @Inject
  public ProjectManagerImpl(@Nonnull Application application, @Nonnull ComponentBinding componentBinding) {
    myApplication = application;
    myComponentBinding = componentBinding;
    myProgressManager = application.getProgressManager();

    MessageBus messageBus = application.getMessageBus();

    MessageBusConnection connection = messageBus.connect();
    myExcludeRootsCache = new ExcludeRootsCache(connection);
    connection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
      @Override
      public void projectOpened(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
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
  }

  @Override
  @RequiredWriteAction
  public void dispose() {
    myApplication.assertWriteAccessAllowed();
  }

  @Override
  @Nullable
  public Project newProject(final String projectName, @Nonnull String dirPath, boolean useDefaultProjectSettings) {
    dirPath = toCanonicalName(dirPath);

    ProjectImpl project = createProject(projectName, dirPath, false);
    try {
      initProject(project, useDefaultProjectSettings ? (ProjectImpl)getDefaultProject() : null);
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
      project.initNotLazyServices();

      ModuleManagerImpl moduleManager = ModuleManagerImpl.getInstanceImpl(project);
      moduleManager.setReady(true);

      succeed = true;
    }
    finally {
      if (!succeed && !project.isDefault()) {
        WriteAction.run(() -> Disposer.dispose(project));
      }
    }
  }

  @Nonnull
  private ProjectImpl createProject(@Nullable String projectName,
                                    @Nonnull String dirPath,
                                    boolean noUICall) {
    return new ProjectImpl(myApplication, this, new File(dirPath).getAbsolutePath(), projectName, noUICall, myComponentBinding);
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
      return myOpenProjects.clone();
    }
  }

  @Override
  public boolean isProjectOpened(Project project) {
    synchronized (lock) {
      return ArrayUtil.contains(project, myOpenProjects);
    }
  }

  private void logStart(Project project) {
    long currentTime = System.nanoTime();
    Long startTime = project.getUserData(ProjectImpl.CREATION_TIME);
    if (startTime != null) {
      LOG.info("Project opening took " + (currentTime - startTime) / 1000000 + " ms");
    }
  }

  private boolean addToOpened(@Nonnull Project project) {
    assert !project.isDisposed() : "Must not open already disposed project";
    synchronized (lock) {
      if (isProjectOpened(project)) {
        return false;
      }
      myOpenProjects = ArrayUtil.append(myOpenProjects, project);
      SingleProjectHolder.theProject = myOpenProjects.length == 1 ? project : null;
    }
    return true;
  }

  @Nonnull
  private Collection<Project> removeFromOpened(@Nonnull Project project) {
    synchronized (lock) {
      myOpenProjects = ArrayUtil.remove(myOpenProjects, project);
      SingleProjectHolder.theProject = myOpenProjects.length == 1 ? myOpenProjects[0] : null;
      return Arrays.asList(myOpenProjects);
    }
  }

  private static boolean canCancelProjectLoading() {
    ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
    return !(indicator instanceof NonCancelableSection);
  }

  @Override
  public void reloadProject(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    doReloadProjectAsync(project, uiAccess);
  }

  public void doReloadProjectAsync(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    ProjectReloadState.getInstance(project).onBeforeAutomaticProjectReload();

    if (project.isDisposed()) {
      return;
    }

    String basePath = project.getBasePath();

    closeAndDisposeAsync(project, uiAccess).doWhenDone(() -> ProjectImplUtil.openAsync(basePath, null, true, uiAccess));
  }

  @Override
  @RequiredUIAccess
  public boolean closeProject(@Nonnull final Project project) {
    return closeProject(project, true, false, true);
  }

  @RequiredUIAccess
  public boolean closeProject(@Nonnull final Project project, final boolean save, final boolean dispose, boolean checkCanClose) {
    if (!isProjectOpened(project)) return true;

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

      myApplication.getMessageBus()
                   .syncPublisher(ProjectManagerListener.class)
                   .projectClosing(project); // somebody can start progress here, do not wrap in write action

      UIAccess uiAccess = UIAccess.current();

      myApplication.runWriteAction(() -> {
        removeFromOpened(project);

        myApplication.getMessageBus().syncPublisher(ProjectManagerListener.class).projectClosed(project, uiAccess);

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
      listeners = project.putUserDataIfAbsent(LISTENERS_IN_PROJECT_KEY, Lists.newLockFreeCopyOnWriteList());
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
      NotificationsManager.getNotificationsManager()
                          .getNotificationsOfType(ProjectStorageUtil.UnableToSaveProjectNotification.class, project);
    if (notifications.length == 0) return true;

    final String fileNames = StringUtil.join(notifications[0].getFileNames(), "\n");

    final String msg = String.format("%s was unable to save some project files,\nare you sure you want to close this project anyway?",
                                     Application.get().getName().getValue());
    return Messages.showDialog(project,
                               msg,
                               "Unsaved Project",
                               "Read-only files:\n\n" + fileNames,
                               new String[]{"Yes", "No"},
                               0,
                               1,
                               Messages.getWarningIcon()) == 0;
  }

  @Nonnull
  @Override
  public AsyncResult<Project> openProjectAsync(@Nonnull VirtualFile file, @Nonnull UIAccess uiAccess) {
    AsyncResult<Project> projectAsyncResult = AsyncResult.undefined();
    Task.Backgroundable.queue(null, "Preparing project...", canCancelProjectLoading(), (indicator) -> {
      tryInitProjectByPath(projectAsyncResult, file, uiAccess);
    });
    return projectAsyncResult;
  }

  @Nonnull
  @Override
  public AsyncResult<Project> openProjectAsync(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    AsyncResult<Project> projectAsyncResult = AsyncResult.undefined();
    loadProjectAsync((ProjectImpl)project, projectAsyncResult, false, uiAccess);
    return projectAsyncResult;
  }

  @Nonnull
  @Override
  public String[] getAllExcludedUrls() {
    return myExcludeRootsCache.getExcludedUrls();
  }

  @Nonnull
  @Override
  public AsyncResult<Void> closeAndDisposeAsync(@Nonnull Project project,
                                                @Nonnull UIAccess uiAccess,
                                                boolean checkCanClose,
                                                boolean save,
                                                boolean dispose) {
    if (!isProjectOpened(project)) {
      return AsyncResult.resolved();
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
        if (save) {
          uiAccess.giveAndWaitIfNeed(() -> {
            FileDocumentManager.getInstance().saveAllDocuments();
            project.save();
          });
        }

        myApplication.getMessageBus()
                     .syncPublisher(ProjectManagerListener.class)
                     .projectClosing(project); // somebody can start progress here, do not wrap in write action

        WriteAction.runAndWait(() -> {
          removeFromOpened(project);

          myApplication.getMessageBus().syncPublisher(ProjectManagerListener.class).projectClosed(project, uiAccess);

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

  private void tryInitProjectByPath(AsyncResult<Project> projectAsyncResult,
                                    VirtualFile path,
                                    UIAccess uiAccess) {
    final ProjectImpl project = createProject(null, toCanonicalName(path.getPath()), true);

    for (Project p : getOpenProjects()) {
      if (ProjectUtil.isSameProject(path.getPath(), p)) {
        uiAccess.give(() -> ProjectWindowFocuser.getInstance(project).focusProjectWindow(false));
        closeAndDisposeAsync(project, uiAccess).doWhenProcessed(() -> projectAsyncResult.reject("Already opened project"));
        return;
      }
    }

    loadProjectAsync(project, projectAsyncResult, true, uiAccess);
  }

  private void loadProjectAsync(final ProjectImpl project,
                                AsyncResult<Project> projectAsyncResult,
                                boolean init,
                                UIAccess uiAccess) {
    Task.Backgroundable.queue(project, ProjectLocalize.projectLoadProgress().get(), canCancelProjectLoading(), progressIndicator -> {
      progressIndicator.setIndeterminate(true);

      try {
        if (!addToOpened(project)) {
          closeAndDisposeAsync(project, uiAccess).doWhenProcessed(() -> projectAsyncResult.reject("Can't add project to opened"));
          return;
        }

        if (init) {
          initProjectAsync(project, null, progressIndicator);
        }

        prepareModules(project, uiAccess, projectAsyncResult).doWhenDone(() -> {
          prepareProjectWorkspace(project, uiAccess, projectAsyncResult);
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

  private AsyncResult<Void> prepareModules(Project project,
                                           UIAccess uiAccess,
                                           AsyncResult<Project> projectAsyncResult) {
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

  private void prepareProjectWorkspace(Project project,
                                       UIAccess uiAccess,
                                       AsyncResult<Project> projectAsyncResult) {
    Task.Backgroundable.queue(project, "Preparing workspace...", canCancelProjectLoading(), progressIndicator -> {
      progressIndicator.setIndeterminate(true);

      try {
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
    myApplication.getMessageBus().syncPublisher(ProjectManagerListener.class).projectOpened(project, uiAccess);

    final StartupManagerImpl startupManager = (StartupManagerImpl)StartupManager.getInstance(project);
    startupManager.runStartupActivities(uiAccess);
    startupManager.runPostStartupActivitiesFromExtensions(uiAccess);

    if (!project.isDisposed()) {
      startupManager.runPostStartupActivities(uiAccess);

      Application application = Application.get();
      if (!application.isHeadlessEnvironment() && !application.isUnitTestMode()) {
        final TrackingPathMacroSubstitutor macroSubstitutor =
          project.getInstance(IProjectStore.class).getStateStorageManager().getMacroSubstitutor();
        if (macroSubstitutor != null) {
          StorageUtil.notifyUnknownMacros(macroSubstitutor, project, null);
        }
      }

      if (application.isActive()) {
        Window projectFrame = WindowManager.getInstance().getWindow(project);
        if (projectFrame != null) {
          uiAccess.giveAndWaitIfNeed(() -> ProjectIdeFocusManager.getInstance(project).requestFocus(projectFrame, true));
        }
      }

      application.invokeLater(() -> {
        if (!project.isDisposedOrDisposeInProgress()) {
          startupManager.scheduleBackgroundPostStartupActivities(uiAccess);

          logStart(project);
        }
      }, IdeaModalityState.nonModal(), project::isDisposedOrDisposeInProgress);
    }
  }

  private void initProjectAsync(@Nonnull final ProjectImpl project,
                                @Nullable ProjectImpl template,
                                ProgressIndicator progressIndicator) throws IOException {
    progressIndicator.setText(ProjectBundle.message("loading.components.for", project.getName()));

    boolean succeed = false;
    try {
      if (template != null) {
        project.getStateStore().loadProjectFromTemplate(template);
      }
      else {
        project.getStateStore().load();
      }
      project.initNotLazyServices();
      succeed = true;
    }
    catch (Throwable e) {
      LOG.error(e);
    }
    finally {
      if (!succeed && !project.isDefault()) {
        project.getUIAccess().give(() -> WriteAction.run(() -> Disposer.dispose(project)));
      }
    }
  }
}
