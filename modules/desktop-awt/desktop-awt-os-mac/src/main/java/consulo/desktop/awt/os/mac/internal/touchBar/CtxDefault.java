// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.execution.executor.Executor;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.project.startup.StartupManager;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Map;

final class CtxDefault {
    private static final Logger LOG = Logger.getInstance(CtxDefault.class);
    private static MessageBusConnection ourConnection = null;

    static void initialize() {
        // 1. load default touchbar actions for all opened projects
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            registerTouchbarActions(project);
        }

        // 2. listen for projects
        ourConnection = ApplicationManager.getApplication().getMessageBus().connect();
        ourConnection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
            @Override
            public void projectOpened(@Nonnull Project project) {
                registerTouchbarActions(project);
            }

            @Override
            public void projectClosed(@Nonnull Project project) {
                LOG.debug("closed project: %s", project);

                final JFrame frame = WindowManager.getInstance().getFrame(project);
                if (frame == null) {
                    // can be when frame is closing (and project is disposing)
                    LOG.debug("null frame for project: %s", project);
                    return;
                }

                TouchBarsManager.unregister(frame); // remove project-default action group
            }
        });

        // 3. schedule to collect run/debug actions
        fillRunDebugGroup();
    }

    static void disable() {
        if (ourConnection != null) {
            ourConnection.disconnect();
        }
        ourConnection = null;
        // NOTE: all registered project actions will 'unregister' in manager.clearAll
        // no necessity to do it here
    }

    private static void registerTouchbarActionsImpl(@Nonnull Project project) {
        if (project.isDisposed()) {
            return;
        }

        final JFrame frame = WindowManager.getInstance().getFrame(project);
        if (frame == null) {
            LOG.debug("null frame for project: %s", project);
            return;
        }

        final @Nullable Pair<Map<Long, ActionGroup>, Customizer> defaultGroup = ActionsLoader.getProjectDefaultActionGroup();
        if (defaultGroup == null) {
            LOG.debug("can't load default action group for project: %s", project);
            TouchBarsManager.unregister(frame);
            return;
        }

        LOG.debug("register project-default action group %s | frame %s", project, frame);
        TouchBarsManager.registerAndShow(frame, defaultGroup.first, defaultGroup.second);
    }

    private static void registerTouchbarActions(@Nonnull Project project) {
        StartupManager.getInstance(project).runAfterOpened(() -> {
            if (project.isDisposed()) {
                return;
            }

            LOG.debug("register touchbar actions for project %s", project);
            registerTouchbarActionsImpl(project);
        });
    }

    static void reloadAllActions() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            registerTouchbarActionsImpl(project);
        }
    }

    private static final String RUN_DEBUG_GROUP_TOUCHBAR = "RunnerActionsTouchbar";

    private static void fillRunDebugGroup() {
        final ActionManager actionManager = ActionManager.getInstance();

        AnAction runButtons = actionManager.getAction(RUN_DEBUG_GROUP_TOUCHBAR);
        if (runButtons == null) {
            LOG.debug("RunnersGroup for touchbar is unregistered");
            return;
        }

        if (!(runButtons instanceof DefaultActionGroup group)) {
            LOG.debug("RunnersGroup for touchbar isn't a group");
            return;
        }

        if (group.getChildrenCount() > 0) {
            LOG.debug("RunnersGroup for touchbar is already filled, skip fill");
            return;
        }

        Application.get().getExtensionPoint(Executor.class).forEach(executor -> {
            if (executor.getId().equals(ToolWindowId.RUN) || executor.getId().equals(ToolWindowId.DEBUG)) {
                group.add(actionManager.getAction(executor.getId()), actionManager);
            }
        });
    }
}
