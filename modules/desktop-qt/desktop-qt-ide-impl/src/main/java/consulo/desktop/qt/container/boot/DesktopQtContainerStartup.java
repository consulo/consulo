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
package consulo.desktop.qt.container.boot;

import consulo.application.impl.internal.start.ApplicationStarter;
import consulo.application.impl.internal.start.StartupAbortedException;
import consulo.application.impl.internal.start.StartupUtil;
import consulo.application.internal.start.StartupActionScriptManager;
import consulo.bootstrap.concurrent.IdeaForkJoinWorkerThreadFactory;
import consulo.container.boot.ContainerPathManager;
import consulo.container.boot.ContainerStartup;
import consulo.container.internal.ShowErrorCaller;
import consulo.container.plugin.PluginManager;
import consulo.container.util.StatCollector;
import consulo.desktop.container.impl.DesktopContainerPathManager;
import consulo.desktop.qt.starter.DesktopQtApplicationStarter;
import consulo.desktop.qt.ui.impl.DesktopQtNativePaths;
import consulo.desktop.startup.DesktopImportantFolderLocker;
import consulo.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtContainerStartup implements ContainerStartup {
    @Override
    public ContainerPathManager createPathManager(Map<String, Object> args) {
        return new DesktopContainerPathManager();
    }

    @Override
    public void run(Map<String, Object> map) {
        StatCollector stat = (StatCollector) map.get(ContainerStartup.STAT_COLLECTOR);
        String[] args = (String[]) map.get(ContainerStartup.ARGS);

        useBundledQtJambiLibraries();

        IdeaForkJoinWorkerThreadFactory.setupForkJoinCommonPool();

        StartupUtil.initializeLogger();

        Runnable appInitializeMark = stat.mark(StatCollector.APP_INITIALIZE);

        Runnable runnable = () -> {
            try {
                start(stat, appInitializeMark, args);
            }
            catch (Throwable t) {
                t.printStackTrace();
                throw new StartupAbortedException(t);
            }
        };

        new Thread(runnable, "Consulo Main Thread").start();
    }

    @Override
    public void destroy() {
    }

    /**
     * The distribution ships the qtjambi libraries unpacked under the native directory of this module. Left to
     * itself qtjambi would instead unpack them out of qtjambi-native-&lt;os&gt;.jar into a temp directory on every
     * start - over 100 mb - and delete them again on exit. Pointing it at the bundled copy skips all of that,
     * and has to happen before any io.qt class initializes, since the search path is read once from a class
     * initializer.
     */
    private static void useBundledQtJambiLibraries() {
        File pluginPath = PluginManager.getPluginPath(DesktopQtContainerStartup.class);
        if (pluginPath == null) {
            return;
        }

        File nativePath = new File(pluginPath, "native");
        // unix builds carry the libraries in lib, windows ones in bin. The override replaces the search path
        // rather than extending it, so java.library.path has to be carried over - qt itself is not bundled and
        // is picked up from wherever the system keeps it
        String searchPath = new File(nativePath, "lib").getAbsolutePath()
            + File.pathSeparator
            + new File(nativePath, "bin").getAbsolutePath()
            + File.pathSeparator
            + System.getProperty("java.library.path", "");

        System.setProperty("io.qt.library-path-override", searchPath);

        File qtPluginPath = new File(nativePath, "plugins");
        if (qtPluginPath.isDirectory()) {
            System.setProperty(DesktopQtNativePaths.PLUGIN_PATH_PROPERTY, qtPluginPath.getAbsolutePath());
        }
    }

    private static void start(StatCollector stat, Runnable appInitializeMark, String[] args) {
        ApplicationStarter.installExceptionHandler(() -> Logger.getInstance(DesktopQtContainerStartup.class));

        try {
            StartupActionScriptManager.executeActionScript();
        }
        catch (IOException e) {
            Logger.getInstance(DesktopQtContainerStartup.class).error(e);

            ShowErrorCaller.showErrorDialog("Plugin Installation Error", e.getMessage(), e);
            return;
        }

        StartupUtil.prepareAndStart(args, stat, DesktopImportantFolderLocker::new, (newConfigFolder, commandLineArgs) -> {
            ApplicationStarter app = new DesktopQtApplicationStarter(commandLineArgs, stat);

            app.run(stat, appInitializeMark, newConfigFolder);
        });
    }
}
