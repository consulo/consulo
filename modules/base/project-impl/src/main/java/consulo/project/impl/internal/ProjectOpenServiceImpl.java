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
import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.progress.ProgressIndicator;
import consulo.component.internal.ComponentBinding;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.ModuleManager;
import consulo.module.impl.internal.ModuleManagerComponent;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.event.ProjectManagerListener;
import consulo.project.internal.ProjectFrameAllocator;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.project.internal.ProjectOpenProcessors;
import consulo.project.internal.ProjectOpenService;
import consulo.project.internal.ProjectOpenSetting;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.localize.ProjectLocalize;
import consulo.project.startup.StartupManager;
import consulo.ui.Alert;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Unified project open service. Handles the full project opening flow:
 * 1. Resolve file path and find appropriate processor
 * 2. Show "same window / new window" dialog if needed
 * 3. Close current project if opening in same window
 * 4. Run processor to prepare project directory (via coroutine chain extension)
 * 5. Create ProjectImpl, allocate frame, load state, init services, load modules
 *
 * @author VISTALL
 * @since 2026-01-29
 */
@ServiceImpl
@Singleton
public class ProjectOpenServiceImpl implements ProjectOpenService {
    private static final Logger LOG = Logger.getInstance(ProjectOpenServiceImpl.class);

    /**
     * Context carrier for coroutine steps.
     */
    private record OpenContext(
        @Nullable Project projectToClose,
        VirtualFile virtualFile
    ) {
    }

    private final Application myApplication;
    private final ProgressBuilderFactory myProgressBuilderFactory;
    private final ComponentBinding myComponentBinding;
    private final ProjectManager myProjectManager;
    private final ProjectFrameAllocator myProjectFrameAllocator;
    private final ProjectOpenProcessors myProjectOpenProcessors;

    @Inject
    public ProjectOpenServiceImpl(Application application,
                                  ProgressBuilderFactory progressBuilderFactory,
                                  ComponentBinding componentBinding,
                                  ProjectManager projectManager,
                                  ProjectFrameAllocator projectFrameAllocator,
                                  ProjectOpenProcessors projectOpenProcessors) {
        myApplication = application;
        myProgressBuilderFactory = progressBuilderFactory;
        myComponentBinding = componentBinding;
        myProjectManager = projectManager;
        myProjectFrameAllocator = projectFrameAllocator;
        myProjectOpenProcessors = projectOpenProcessors;
    }

    @Override
    public CompletableFuture<Project> openProjectAsync(
        Path filePath,
        UIAccess uiAccess,
        ProjectOpenContext context) {

        // Pre-resolve VirtualFile and processor before chain construction
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(filePath);
        if (virtualFile == null) {
            return CompletableFuture.completedFuture(null);
        }

        ProjectOpenProcessor processor = myProjectOpenProcessors.findProcessor(VirtualFileUtil.virtualToIoFile(virtualFile));

        Boolean forceNewFrame = context.getUserData(ProjectOpenContext.FORCE_OPEN_IN_NEW_FRAME);
        Project activeProject = context.getUserData(ProjectOpenContext.ACTIVE_PROJECT);

        CompletableFuture<Project> resultFuture = new CompletableFuture<>();

        final VirtualFile finalVirtualFile = virtualFile;
        final ProjectOpenProcessor finalProcessor = processor;

        // Phase 1: Resolve project, show dialog if needed, allocate frame
        // Use null project for progress scope — activeProject may be closed during this phase
        CompletableFuture<ProjectImpl> projectInit = myProgressBuilderFactory
            .newProgressBuilder(null, ProjectLocalize.projectLoadProgress())
            .cancelable()
            .execute(uiAccess, () -> {
                Coroutine<?, OpenContext> baseChain = Coroutine
                    .first(CodeExecution.<Void, OpenContext>apply((input, continuation) -> {
                        Project projectToClose = null;
                        if (!Boolean.TRUE.equals(forceNewFrame)) {
                            Project[] openProjects = myProjectManager.getOpenProjects();
                            if (openProjects.length > 0) {
                                projectToClose = activeProject != null ? activeProject : openProjects[openProjects.length - 1];
                            }
                        }
                        return new OpenContext(projectToClose, finalVirtualFile);
                    }))
                    // Handle "same window / new window" dialog and close if needed
                    .then(CompletableFutureStep.<OpenContext, OpenContext>await(openContext -> {
                        if (openContext.projectToClose() == null) {
                            return CompletableFuture.completedFuture(openContext);
                        }

                        return confirmAndCloseProjectAsync(openContext, uiAccess);
                    }))
                    // Cancel if dialog was cancelled (null means cancelled)
                    .then(CodeExecution.<OpenContext, OpenContext>apply((openContext, continuation) -> {
                        if (openContext == null) {
                            continuation.cancel();
                            return null;
                        }
                        return openContext;
                    }));

                // Extract VirtualFile from OpenContext for processor
                Coroutine<?, VirtualFile> preProcessor = baseChain
                    .then(CodeExecution.apply(OpenContext::virtualFile));

                // Let processor extend chain with preparation steps (default: no-op)
                Coroutine<?, VirtualFile> withProcessor = finalProcessor != null
                    ? finalProcessor.prepareSteps(uiAccess, context, preProcessor)
                    : preProcessor;

                // Create ProjectImpl from VirtualFile
                Coroutine<?, ProjectImpl> chain = withProcessor
                    .then(CodeExecution.<VirtualFile, ProjectImpl>apply((projectDir, continuation) -> {
                        if (projectDir == null) {
                            continuation.cancel();
                            return null;
                        }
                        return new ProjectImpl(myApplication, myProjectManager, projectDir.getPath(), null, true, myComponentBinding);
                    }));

                // Allocate frame
                return myProjectFrameAllocator.allocateFrame(context, chain);
            });

        // Phase 2: With the ProjectImpl, load state, init services, load modules, etc.
        projectInit.whenComplete((projectImpl, throwable) -> {
            if (throwable != null) {
                LOG.warn("Failed to open project: " + filePath, throwable);
                resultFuture.completeExceptionally(throwable);
            }
            else if (projectImpl == null) {
                resultFuture.completeExceptionally(new FileNotFoundException("Failed to open project: " + filePath));
            }
            else {
                doOpenInProject(projectImpl, filePath, uiAccess, context, resultFuture);
            }
        });

        return resultFuture;
    }

    private void doOpenInProject(ProjectImpl project, Path filePath, UIAccess uiAccess,
                                 ProjectOpenContext context, CompletableFuture<Project> future) {
        myProgressBuilderFactory.newProgressBuilder(project, ProjectLocalize.projectLoadProgress())
            .cancelable()
            .execute(uiAccess, () -> {
                Coroutine<?, Object> init = Coroutine
                    .first(CodeExecution.run(() -> {
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

                ProgressIndicator[] moduleIndicator = new ProgressIndicator[1];
                init = init
                    .then(CodeExecution.apply((o, c) -> {
                        ProgressIndicator indicator = ProgressIndicator.from(c);
                        indicator.setText(ProjectLocalize.progressTitleLoadingModules());
                        indicator.setText2(LocalizeValue.empty());
                        moduleIndicator[0] = indicator;
                        return o;
                    }))
                    .then(CompletableFutureStep.await((o) -> {
                        ModuleManagerComponent moduleManager = (ModuleManagerComponent) ModuleManager.getInstance(project);
                        return moduleManager.loadModules(moduleIndicator[0]).thenApply(x -> o);
                    }));

                init = init.then(CodeExecution.supply((o) -> {
                        ProgressIndicator indicator = ProgressIndicator.from(o);

                        indicator.setText(ProjectLocalize.progressTitlePreparingWorkspace());
                        indicator.setText2(LocalizeValue.empty());

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
                    LOG.warn("Failed to initialize project: " + filePath, throwable);
                    future.completeExceptionally(throwable);
                }
                else {
                    updateLastProjectLocation(filePath.toString());
                    future.complete(project);
                }
            });
    }

    /**
     * Show "same window / new window" dialog, close current project if needed.
     *
     * @return the same OpenContext if we should proceed, or null if cancelled
     */
    private CompletableFuture<OpenContext> confirmAndCloseProjectAsync(
        OpenContext openContext,
        UIAccess uiAccess) {

        ProjectOpenSetting settings = ProjectOpenSetting.getInstance();
        int confirmOpenNewProject = settings.getConfirmOpenNewProject();

        if (confirmOpenNewProject == ProjectOpenSetting.OPEN_PROJECT_NEW_WINDOW) {
            // Always open in new window - no need to close anything
            return CompletableFuture.completedFuture(
                new OpenContext(null, openContext.virtualFile())
            );
        }

        if (confirmOpenNewProject == ProjectOpenSetting.OPEN_PROJECT_SAME_WINDOW) {
            // Always use same window - close current project
            return closeAndProceed(openContext, uiAccess);
        }

        // ASK: show dialog
        CompletableFuture<OpenContext> result = new CompletableFuture<>();

        Alert<Integer> alert = Alert.create();
        alert.asQuestion();
        alert.remember(ProjectNewWindowDoNotAskOption.INSTANCE);
        alert.title(ProjectLocalize.titleOpenProject());
        alert.text(ProjectLocalize.promptOpenProjectInNewFrame());

        alert.button(ProjectLocalize.buttonExistingframe(), () -> ProjectOpenSetting.OPEN_PROJECT_SAME_WINDOW);
        alert.asDefaultButton();

        alert.button(ProjectLocalize.buttonNewframe(), () -> ProjectOpenSetting.OPEN_PROJECT_NEW_WINDOW);

        alert.button(Alert.CANCEL, Alert.CANCEL);
        alert.asExitButton();

        uiAccess.give(() -> {
            Project projectToClose = openContext.projectToClose();
            if (projectToClose != null) {
                alert.showAsync(projectToClose).doWhenDone(exitCode -> {
                    handleDialogResult(exitCode, openContext, uiAccess, result);
                });
            }
            else {
                alert.showAsync().doWhenDone(exitCode -> {
                    handleDialogResult(exitCode, openContext, uiAccess, result);
                });
            }
        });

        return result;
    }

    private void handleDialogResult(int exitCode,
                                    OpenContext openContext,
                                    UIAccess uiAccess,
                                    CompletableFuture<OpenContext> result) {
        if (exitCode == ProjectOpenSetting.OPEN_PROJECT_SAME_WINDOW) {
            closeAndProceed(openContext, uiAccess).whenComplete((ctx, error) -> {
                if (error != null) {
                    result.completeExceptionally(error);
                }
                else {
                    result.complete(ctx);
                }
            });
        }
        else if (exitCode == ProjectOpenSetting.OPEN_PROJECT_NEW_WINDOW) {
            // New window - don't close anything
            result.complete(
                new OpenContext(null, openContext.virtualFile())
            );
        }
        else {
            // Cancel - return null, the next CodeExecution step will call continuation.cancel()
            result.complete(null);
        }
    }

    private CompletableFuture<OpenContext> closeAndProceed(
        OpenContext openContext,
        UIAccess uiAccess) {

        CompletableFuture<OpenContext> result = new CompletableFuture<>();

        if (openContext.projectToClose() == null) {
            result.complete(openContext);
            return result;
        }

        myProjectManager.closeAndDisposeAsync(openContext.projectToClose(), uiAccess)
            .whenComplete((closed, error) -> {
                if (error != null) {
                    result.completeExceptionally(error);
                }
                else if (Boolean.TRUE.equals(closed)) {
                    result.complete(
                        new OpenContext(null, openContext.virtualFile())
                    );
                }
                else {
                    // Close was rejected - return null, the next CodeExecution step will cancel
                    result.complete(null);
                }
            });

        return result;
    }

    private static void updateLastProjectLocation(String projectFilePath) {
        File lastProjectLocation = new File(projectFilePath);
        if (lastProjectLocation.isFile()) {
            lastProjectLocation = lastProjectLocation.getParentFile();
        }
        if (lastProjectLocation == null) {
            return;
        }
        lastProjectLocation = lastProjectLocation.getParentFile();
        if (lastProjectLocation == null) {
            return;
        }
        String path = lastProjectLocation.getPath();
        try {
            path = FileUtil.resolveShortWindowsName(path);
        }
        catch (IOException e) {
            LOG.info(e);
            return;
        }
        RecentProjectsManager.getInstance().setLastProjectCreationLocation(path.replace(File.separatorChar, '/'));
    }
}
