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
package consulo.continuousIntegration.impl.internal.monitor;

import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.continuousIntegration.CIBuild;
import consulo.continuousIntegration.CIBuildMonitor;
import consulo.continuousIntegration.CIBuildStatus;
import consulo.continuousIntegration.CICapability;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CIRunRequest;
import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.impl.internal.CINotificationGroupContributor;
import consulo.continuousIntegration.impl.internal.CIUtil;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.continuousIntegration.support.CIRerunSupport;
import consulo.disposer.Disposable;
import consulo.execution.RunContentExecutor;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.ui.RunContentManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.proxy.EventDispatcher;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.webBrowser.BrowserUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Shows each followed build as a {@link CIBuildProcessHandler} in its own tab of the Run tool window.
 *
 * @since 2026-11-09
 */
@Singleton
@ServiceImpl
public class CIBuildMonitorImpl implements CIBuildMonitor {
    private final Project myProject;
    private final ApplicationConcurrency myConcurrency;
    private final NotificationService myNotificationService;
    private final Provider<RunContentManager> myRunContentManager;
    private final Provider<ToolWindowManager> myToolWindowManager;
    private final Map<String, CIBuildProcessHandler> myHandlers = new ConcurrentHashMap<>();
    private final EventDispatcher<CIBuildMonitorListener> myDispatcher = EventDispatcher.create(CIBuildMonitorListener.class);

    @Inject
    public CIBuildMonitorImpl(
        Project project,
        ApplicationConcurrency concurrency,
        NotificationService notificationService,
        Provider<RunContentManager> runContentManager,
        Provider<ToolWindowManager> toolWindowManager
    ) {
        myProject = project;
        myConcurrency = concurrency;
        myNotificationService = notificationService;
        myRunContentManager = runContentManager;
        myToolWindowManager = toolWindowManager;
    }

    public void addListener(CIBuildMonitorListener listener, Disposable parentDisposable) {
        myDispatcher.addListener(listener, parentDisposable);
    }

    @Override
    public CompletableFuture<CIBuild> runBuild(CIServer server, CIJob job, CIRunRequest request) {
        CompletableFuture<CIBuild> started = call(() -> server.runBuild(job, request));
        follow(server, job, started, null, LocalizeValue.of(job.displayName()), () -> runBuild(server, job, request));
        return started;
    }

    @Override
    public void showBuild(CIServer server, CIJob job, CIBuild build) {
        myProject.getUIAccess().give(() -> {
            if (myProject.isDisposed()) {
                return;
            }

            CIBuildProcessHandler existing = myHandlers.get(CIUtil.buildKey(server, build));
            if (existing != null && !existing.isProcessTerminated()) {
                toFront(existing);
                return;
            }

            CIBuildProcessHandler handler = createHandler(server, job, CompletableFuture.completedFuture(build), build);
            LocalizeValue title = ContinuousIntegrationImplLocalize.monitorTitle(job.displayName(), build.displayName());
            show(handler, title, rerun(server, job, build));
        });
    }

    /**
     * Stops a build: through its console when it is followed, so the console shows what happens, otherwise directly.
     */
    public CompletableFuture<?> stopBuild(CIServer server, CIJob job, CIBuild build) {
        CIBuildProcessHandler handler = myHandlers.get(CIUtil.buildKey(server, build));
        if (handler != null && !handler.isProcessTerminated()) {
            handler.destroyProcess();
            return CompletableFuture.completedFuture(null);
        }

        return call(() -> server.stopBuild(build)).whenComplete((result, error) -> {
            if (error == null) {
                myDispatcher.getMulticaster().buildChanged(server, job, build);
            }
        });
    }

    private void follow(
        CIServer server,
        CIJob job,
        CompletableFuture<CIBuild> started,
        @Nullable CIBuild attachedBuild,
        LocalizeValue title,
        @Nullable Runnable rerun
    ) {
        myProject.getUIAccess().give(() -> {
            if (!myProject.isDisposed()) {
                show(createHandler(server, job, started, attachedBuild), title, rerun);
            }
        });
    }

    private CIBuildProcessHandler createHandler(
        CIServer server,
        CIJob job,
        CompletableFuture<CIBuild> start,
        @Nullable CIBuild attachedBuild
    ) {
        return new CIBuildProcessHandler(this, server, job, start, attachedBuild, myConcurrency.getScheduledExecutorService());
    }

    private @Nullable Runnable rerun(CIServer server, CIJob job, CIBuild build) {
        CIRerunSupport rerunSupport = server.getSupport(CIRerunSupport.class);
        if (rerunSupport != null) {
            return () -> {
                CompletableFuture<CIBuild> started = call(() -> rerunSupport.rerun(build, false));
                follow(server, job, started, null, LocalizeValue.of(job.displayName()), rerun(server, job, build));
            };
        }
        if (job.buildable() && !job.hasParameters() && server.hasCapability(CICapability.RUN)) {
            return () -> runBuild(server, job, CIRunRequest.DEFAULT);
        }
        return null;
    }

    @RequiredUIAccess
    private void show(CIBuildProcessHandler handler, LocalizeValue title, @Nullable Runnable rerun) {
        RunContentExecutor executor = new RunContentExecutor(myProject, handler)
            .withTitle(title.get())
            .withIcon(handler.getServer().getType().getIcon())
            .withStop(handler::destroyProcess, handler::canStop);
        if (rerun != null) {
            executor.withRerun(rerun);
        }
        executor.run();
    }

    @RequiredUIAccess
    private void toFront(CIBuildProcessHandler handler) {
        myRunContentManager.get().toFrontRunContent(DefaultRunExecutor.getRunExecutorInstance(), handler);
        ToolWindow toolWindow = myToolWindowManager.get().getToolWindow(ToolWindowId.RUN);
        if (toolWindow != null) {
            toolWindow.activate(null);
        }
    }

    void buildStarted(CIBuildProcessHandler handler, CIBuild build) {
        myHandlers.put(CIUtil.buildKey(handler.getServer(), build), handler);
        myDispatcher.getMulticaster().buildChanged(handler.getServer(), handler.getJob(), build);
    }

    void buildChanged(CIBuildProcessHandler handler, CIBuild build) {
        myDispatcher.getMulticaster().buildChanged(handler.getServer(), handler.getJob(), build);
    }

    void buildFinished(CIBuildProcessHandler handler, @Nullable CIBuild build, boolean notify) {
        if (build == null) {
            return;
        }

        myHandlers.remove(CIUtil.buildKey(handler.getServer(), build), handler);
        myDispatcher.getMulticaster().buildChanged(handler.getServer(), handler.getJob(), build);
        if (notify && !myProject.isDisposed()) {
            notifyFinished(handler, build);
        }
    }

    private void notifyFinished(CIBuildProcessHandler handler, CIBuild build) {
        CIBuildStatus status = build.status();
        Notification.Builder builder = switch (status) {
            case SUCCESS -> myNotificationService.newSuccess(CINotificationGroupContributor.BUILDS);
            case FAILURE -> myNotificationService.newError(CINotificationGroupContributor.BUILDS);
            case UNSTABLE -> myNotificationService.newWarn(CINotificationGroupContributor.BUILDS);
            default -> myNotificationService.newInfo(CINotificationGroupContributor.BUILDS);
        };

        builder
            .title(ContinuousIntegrationImplLocalize.notificationBuildFinishedTitle(handler.getJob().displayName(), build.displayName()))
            .content(ContinuousIntegrationImplLocalize.notificationBuildFinishedContent(status.getPresentableName()))
            .addClosingAction(ContinuousIntegrationImplLocalize.notificationActionShowLog(), () -> {
                if (myProject.isDisposed()) {
                    return;
                }
                // the tab may be closed meanwhile: then the log is read again into a new one
                if (myRunContentManager.get().findContentDescriptor(DefaultRunExecutor.getRunExecutorInstance(), handler) != null) {
                    toFront(handler);
                }
                else {
                    showBuild(handler.getServer(), handler.getJob(), build);
                }
            });

        String webUrl = build.webUrl();
        if (webUrl != null) {
            LocalizeValue openInBrowser = ContinuousIntegrationImplLocalize.notificationActionOpenInBrowser();
            builder.addClosingAction(openInBrowser, () -> BrowserUtil.browse(webUrl));
        }
        builder.notify(myProject);
    }

    private static <T> CompletableFuture<T> call(Supplier<CompletableFuture<T>> request) {
        try {
            return request.get();
        }
        catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
