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
package consulo.desktop.qt.starter;

import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.start.ApplicationStarter;
import consulo.application.impl.internal.start.CommandLineArgs;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.StartupProgress;
import consulo.component.internal.ComponentBinding;
import consulo.container.util.StatCollector;
import consulo.desktop.qt.application.impl.DesktopQtApplicationImpl;
import consulo.externalService.statistic.UsageTrigger;
import consulo.ide.impl.idea.ide.CommandLineProcessor;
import consulo.ide.impl.idea.ide.RecentProjectsManagerImpl;
import consulo.ide.impl.idea.openapi.wm.impl.SystemDock;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.style.StyleManagerService;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtApplicationStarter extends ApplicationStarter {
    private static final Logger LOG = Logger.getInstance(DesktopQtApplicationStarter.class);

    public DesktopQtApplicationStarter(CommandLineArgs commandLineArgs, StatCollector stat) {
        super(commandLineArgs, stat);
    }

    @Override
    public @Nullable StartupProgress createSplash(CommandLineArgs args) {
        return null;
    }

    @Override
    protected Application createApplication(
        ComponentBinding componentBinding,
        boolean isHeadlessMode,
        SimpleReference<StartupProgress> splashRef,
        CommandLineArgs args
    ) {
        return new DesktopQtApplicationImpl(componentBinding, splashRef);
    }

    @Override
    protected void main(
        StatCollector stat,
        Runnable appInitializeMark,
        ApplicationEx app,
        boolean newConfigFolder,
        CommandLineArgs args
    ) {
        appInitializeMark.run();

        SystemDock.getInstance().updateMenu();

        RecentProjectsManagerImpl recentProjectsManager = (RecentProjectsManagerImpl) RecentProjectsManager.getInstance();

        // a project reopened at startup raises a frame of its own, and the welcome screen would only stand behind
        // it - so it is shown for the case where nothing is going to be reopened
        if (args.isNoRecentProjects() || !recentProjectsManager.willReopenProjectOnStart()) {
            app.invokeLater(() -> WelcomeFrameManager.getInstance().showFrame(), IdeaModalityState.any());
        }

        app.invokeLater(
            () -> {
                if (!args.isNoRecentProjects()) {
                    openProject(recentProjectsManager, args);
                }

                UsageTrigger.trigger("consulo.app.started");
            },
            app.getNoneModalityState()
        );

        stat.dump("Startup statistics", LOG::info);
    }

    /**
     * What the ide comes up on: the project named on the command line, and the ones which were open when it was
     * last closed for anything else.
     */
    @RequiredUIAccess
    private void openProject(RecentProjectsManagerImpl recentProjectsManager, CommandLineArgs args) {
        AsyncResult<Project> projectFromCommandLine = AsyncResult.rejected();

        if (isPerformProjectLoad()) {
            try {
                projectFromCommandLine = CommandLineProcessor.processExternalCommandLine(args, null);
            }
            catch (Throwable e) {
                LOG.warn("Failed to open project from command line: " + args.getFile(), e);

                projectFromCommandLine = AsyncResult.rejected();
            }
        }

        projectFromCommandLine.doWhenRejected(recentProjectsManager::doReopenLastProject);
    }
}
