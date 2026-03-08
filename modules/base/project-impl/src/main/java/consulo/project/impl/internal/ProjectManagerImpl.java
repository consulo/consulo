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
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.component.internal.ComponentBinding;
import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.module.impl.internal.ModuleManagerImpl;
import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.project.event.ProjectManagerListener;
import consulo.project.internal.ProjectManagerEx;
import consulo.project.internal.ProjectOpenService;
import consulo.project.internal.ProjectReloadState;
import consulo.project.internal.SingleProjectHolder;
import consulo.project.localize.ProjectLocalize;
import consulo.proxy.EventDispatcher;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Singleton
@ServiceImpl
public class ProjectManagerImpl implements ProjectManagerEx, Disposable {
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
    private final Provider<ProjectOpenService> myProjectOpenService;
    @Nonnull
    private final ProgressIndicatorProvider myProgressManager;

    private final EventDispatcher<ProjectManagerListener> myDeprecatedListenerDispatcher =
        EventDispatcher.create(ProjectManagerListener.class);

    @Nonnull
    private static List<ProjectManagerListener> getListeners(Project project) {
        List<ProjectManagerListener> array = project.getUserData(LISTENERS_IN_PROJECT_KEY);
        if (array == null) {
            return Collections.emptyList();
        }
        return array;
    }

    private ExcludeRootsCache myExcludeRootsCache;

    @Inject
    public ProjectManagerImpl(@Nonnull Application application,
                              @Nonnull ComponentBinding componentBinding,
                              @Nonnull Provider<ProjectOpenService> projectOpenService) {
        myApplication = application;
        myComponentBinding = componentBinding;
        myProjectOpenService = projectOpenService;
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

    @Nonnull
    @Override
    public CompletableFuture<Project> openProjectAsync(@Nonnull Path filePath, @Nonnull UIAccess uiAccess, @Nonnull ProjectOpenContext context) {
        return myProjectOpenService.get().openProjectAsync(filePath, uiAccess, context);
    }

    @Override
    @RequiredWriteAction
    public void dispose() {
        myApplication.assertWriteAccessAllowed();
    }

    @Nullable
    @Override
    @RequiredUIAccess
    public Project newProject(String projectName, @Nonnull String dirPath, boolean useDefaultProjectSettings) {
        dirPath = toCanonicalName(dirPath);

        ProjectImpl project = createProject(projectName, dirPath, false);
        try {
            initProject(project, useDefaultProjectSettings ? (ProjectImpl) getDefaultProject() : null);
            return project;
        }
        catch (Throwable t) {
            LOG.info(t);
            Messages.showErrorDialog(message(t), ProjectLocalize.projectLoadDefaultError().get());
            return null;
        }
    }

    @Nonnull
    private static String message(Throwable e) {
        String message = e.getMessage();
        if (message != null) {
            return message;
        }
        message = e.getLocalizedMessage();
        //noinspection ConstantConditions
        if (message != null) {
            return message;
        }
        message = e.toString();
        Throwable cause = e.getCause();
        if (cause != null) {
            String causeMessage = message(cause);
            return message + " (cause: " + causeMessage + ")";
        }

        return message;
    }

    private void initProject(@Nonnull ProjectImpl project, @Nullable ProjectImpl template) throws IOException {
        ProgressIndicator indicator = myProgressManager.getProgressIndicator();
        if (indicator != null && !project.isDefault()) {
            indicator.setTextValue(ProjectLocalize.loadingComponentsFor(project.getName()));
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

            succeed = true;
        }
        finally {
            if (!succeed && !project.isDefault()) {
                WriteAction.run(() -> Disposer.dispose(project));
            }
        }
    }

    @Nonnull
    private ProjectImpl createProject(
        @Nullable String projectName,
        @Nonnull String dirPath,
        boolean noUICall
    ) {
        return new ProjectImpl(myApplication, this, new File(dirPath).getAbsolutePath(), projectName, noUICall, myComponentBinding);
    }

    @Nonnull
    private static String toCanonicalName(@Nonnull String filePath) {
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

    public boolean addToOpened(@Nonnull Project project) {
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
    Collection<Project> removeFromOpened(@Nonnull Project project) {
        synchronized (lock) {
            myOpenProjects = ArrayUtil.remove(myOpenProjects, project);
            SingleProjectHolder.theProject = myOpenProjects.length == 1 ? myOpenProjects[0] : null;
            return Arrays.asList(myOpenProjects);
        }
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

        closeProjectAsync(project, uiAccess).whenComplete((closed, error) -> {
            if (error == null && closed) {
                ProjectOpenContext ctx = new ProjectOpenContext();
                ctx.putUserData(ProjectOpenContext.FORCE_OPEN_IN_NEW_FRAME, true);
                myProjectOpenService.get().openProjectAsync(Path.of(basePath), uiAccess, ctx);
            }
        });
    }

    @Override
    public void addProjectManagerListener(@Nonnull ProjectManagerListener listener) {
        myDeprecatedListenerDispatcher.addListener(listener);
    }

    @Override
    public void addProjectManagerListener(@Nonnull ProjectManagerListener listener, @Nonnull Disposable parentDisposable) {
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
                if (!listener.test(project)) {
                    return false;
                }
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


    @Nonnull
    @Override
    public String[] getAllExcludedUrls() {
        return myExcludeRootsCache.getExcludedUrls();
    }

}
