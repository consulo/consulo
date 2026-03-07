/*
 * Copyright 2013-2026 consulo.io
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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.progress.ProgressIndicator;
import consulo.component.internal.ComponentBinding;
import consulo.localize.LocalizeValue;
import consulo.module.ModuleManager;
import consulo.module.impl.internal.ModuleManagerComponent;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.event.ProjectManagerListener;
import consulo.project.internal.ProjectFrameAllocator;
import consulo.project.internal.ProjectOpenService;
import consulo.project.localize.ProjectLocalize;
import consulo.project.startup.StartupManager;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-01-29
 */
@ServiceImpl
@Singleton
public class ProjectOpenServiceImpl implements ProjectOpenService {
    @Nonnull
    private final Application myApplication;
    private final ProgressBuilderFactory myProgressBuilderFactory;
    @Nonnull
    private final ComponentBinding myComponentBinding;
    @Nonnull
    private final ProjectManager myProjectManager;
    private ProjectFrameAllocator myProjectFrameAllocator;

    @Inject
    public ProjectOpenServiceImpl(@Nonnull Application application,
                                  @Nonnull ProgressBuilderFactory progressBuilderFactory,
                                  @Nonnull ComponentBinding componentBinding,
                                  @Nonnull ProjectManager projectManager,
                                  @Nonnull ProjectFrameAllocator projectFrameAllocator) {
        myApplication = application;
        myProgressBuilderFactory = progressBuilderFactory;
        myComponentBinding = componentBinding;
        myProjectManager = projectManager;
        myProjectFrameAllocator = projectFrameAllocator;
    }

    @Nonnull
    @Override
    public CompletableFuture<Project> openProjectAsync(
        @Nonnull Path filePath,
        @Nonnull UIAccess uiAccess,
        @Nonnull ProjectOpenContext context) {
        CompletableFuture<Project> future = new CompletableFuture<>();

        Project activeProject = context.getUserData(ProjectOpenContext.ACTIVE_PROJECT);

        CompletableFuture<ProjectImpl> projectInit = myProgressBuilderFactory.newProgressBuilder(activeProject, ProjectLocalize.projectLoadProgress())
            .cancelable()
            .execute(uiAccess, first -> {
                Coroutine<?, ProjectImpl> then = first.then(CodeExecution.apply((o, continuation) -> {
                    VirtualFile projectDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(filePath);
                    if (projectDir != null) {
                        return new ProjectImpl(myApplication, myProjectManager, projectDir, null, myComponentBinding);
                    }
                    else {
                        continuation.cancel();
                    }
                    return null;
                }));

                return myProjectFrameAllocator.allocateFrame(context, then);
            });

        projectInit.whenComplete((projectImpl, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            }
            else if (projectImpl == null) {
                future.completeExceptionally(new FileNotFoundException("File not found : " + filePath));
            }
            else {
                doOpenInProject(projectImpl, uiAccess, future);
            }
        });

        return future;
    }

    private void doOpenInProject(ProjectImpl project, UIAccess uiAccess, CompletableFuture<Project> future) {
        myProgressBuilderFactory.newProgressBuilder(project, ProjectLocalize.projectLoadProgress())
            .cancelable()
            .execute(uiAccess, first -> {
                Coroutine<?, Object> init = first
                    .then(CodeExecution.run(() -> {
                        try {
                            project.getStateStore().load();
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .then(CodeExecution.supply((c) -> {
                        project.initNotLazyServices();
                        return c;
                    }))
                    .then(CodeExecution.supply((c) -> {
                        ProjectManagerImpl manager = (ProjectManagerImpl) ProjectManager.getInstance();
                        manager.addToOpened(project);

                        myApplication.getMessageBus().syncPublisher(ProjectManagerListener.class).projectOpened(project, uiAccess);

                        return c;
                    }));

                init = myProjectFrameAllocator.initializeSteps(project, init);

                init = init.then(WriteLock.apply((o, c) -> {
                        ProgressIndicator indicator = ProgressIndicator.from(c);
                        indicator.setTextValue(ProjectLocalize.progressTitleLoadingModules());
                        indicator.setText2Value(LocalizeValue.empty());

                        ModuleManagerComponent moduleManager = (ModuleManagerComponent) ModuleManager.getInstance(project);
                        moduleManager.loadModulesNew(indicator);
                        return o;
                    }))
                    .then(CodeExecution.supply((o) -> {
                        ProgressIndicator indicator = ProgressIndicator.from(o);

                        indicator.setTextValue(ProjectLocalize.progressTitlePreparingWorkspace());
                        indicator.setText2Value(LocalizeValue.empty());

                        StartupManagerImpl startupManager = (StartupManagerImpl) StartupManager.getInstance(project);
                        startupManager.runPostStartupActivitiesFromExtensions(uiAccess);

                        project.setFullyInitialized(true);
                        return o;
                    }))
                    .then(CodeExecution.supply((o) -> {
                        StartupManagerImpl startupManager = (StartupManagerImpl) StartupManager.getInstance(project);
                        startupManager.runPostStartupActivities(uiAccess);
                        return o;
                    }))
                    .then(CodeExecution.supply(o -> {
                        StartupManagerImpl startupManager = (StartupManagerImpl) StartupManager.getInstance(project);
                        startupManager.scheduleBackgroundPostStartupActivities(uiAccess);
                        return o;
                    }));

                init = myProjectFrameAllocator.postSteps(project, init);
                return init;
            }).whenComplete((o, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
                else if (project == null) {
                    future.completeExceptionally(new IllegalArgumentException("Failed to open project"));
                }
                else {
                    future.complete(project);
                }
            });
    }
}
