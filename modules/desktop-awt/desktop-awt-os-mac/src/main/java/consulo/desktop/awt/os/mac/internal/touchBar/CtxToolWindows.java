// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.collection.WeakList;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class CtxToolWindows {
    private static final Logger LOG = Logger.getInstance(CtxToolWindows.class);
    private static MessageBusConnection ourConnection = null;
    private static final WeakList<MessageBusConnection> ourProjConnections = new WeakList<>();

    static void initialize() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            subscribeToolWindowTopic(project);
        }

        ourConnection = ApplicationManager.getApplication().getMessageBus().connect();
        ourConnection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
            @Override
            public void projectOpened(@Nonnull Project project) {
                subscribeToolWindowTopic(project);
            }

            @Override
            public void projectClosed(@Nonnull Project project) {
                forEachToolWindow(project, tw -> {
                    if (tw != null)
                        TouchBarsManager.unregister(tw.getComponent());
                });
            }
        });
    }

    static synchronized void disable() {
        if (ourConnection != null)
            ourConnection.disconnect();
        ourConnection = null;

        ourProjConnections.forEach(mbc -> mbc.disconnect());
        ourProjConnections.clear();

        // NOTE: all registered project actions will 'unregister' in manager.clearAll
        // no necessity to do it here
    }

    private static void forEachToolWindow(@Nonnull Project project, Consumer<? super ToolWindow> func) {
        ToolWindowManagerEx toolWindowManager = ToolWindowManagerEx.getInstanceEx(project);
        for (ToolWindow window : toolWindowManager.getToolWindows()) {
            func.accept(window);
        }
    }

    private static void subscribeToolWindowTopic(@Nonnull Project project) {
        if (project.isDisposed()) {
            return;
        }


        LOG.debug("subscribe for ToolWindow topic of project %s", project);
        MessageBusConnection pbc = project.getMessageBus().connect();
        pbc.subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
            @Override
            public void toolWindowsRegistered(@Nonnull List<String> ids, @Nonnull ToolWindowManager toolWindowManager) {
                for (String id : ids) {
                    final @Nullable Pair<Map<Long, ActionGroup>, Customizer> actions = ActionsLoader.getToolWindowActionGroup(id);
                    if (actions == null || actions.first.get(0L) == null) {
                        LOG.debug("null action group (or it doesn't contain main-layout) for tool window: %s", id);
                        continue;
                    }

                    ToolWindow toolWindow = toolWindowManager.getToolWindow(id);
                    if (toolWindow == null)
                        continue;
                    TouchBarsManager.register(toolWindow.getComponent(), actions.first, actions.second);
                    LOG.debug("register tool-window '%s' for component: %s", id, toolWindow.getComponent());
                }
            }

            @Override
            public void toolWindowUnregistered(@Nonnull String id, @Nonnull ToolWindow toolWindow) {
                TouchBarsManager.unregister(toolWindow.getComponent());
            }
        });

        ourProjConnections.add(pbc);
    }

    static void reloadAllActions() {
        for (Project p : ProjectManager.getInstance().getOpenProjects()) {
            if (p.isDisposed()) {
                continue;
            }
            forEachToolWindow(p, tw -> {
                if (tw == null)
                    return;

                final @Nullable Pair<Map<Long, ActionGroup>, Customizer> actions = ActionsLoader.getToolWindowActionGroup(tw.getId());
                if (actions == null || actions.first.get(0L) == null) {
                    LOG.debug("reloaded null action group (or it doesn't contain main-layout) for tool window: %s", tw.getId());
                    return;
                }

                TouchBarsManager.register(tw.getComponent(), actions.first, actions.second);
                LOG.debug("re-register tool-window '%s' for component: %s", tw.getId(), tw.getComponent());
            });
        }
    }
}
