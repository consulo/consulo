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
package consulo.desktop.qt.ui.impl;

import io.qt.core.QCoreApplication;

import java.util.List;

/**
 * Where the distribution keeps the qt plugins it ships, and how qt is told about them.
 * <p/>
 * A bundled qt is not the one it was built as: the plugin directory {@code QLibraryInfo} carries is the prefix of
 * the machine which compiled qt, so a frontend running off the libraries in the distribution finds no platform
 * plugin there and aborts with "no qt platform plugin could be initialized". The search path is stated here
 * instead, and it has to be stated before the platform plugin is looked for, which is while
 * {@code QApplication.initialize} runs.
 * <p/>
 * Nothing is applied when the property is unset - a frontend running against a qt of the system keeps the paths
 * that qt was built with, which are the right ones there.
 *
 * @author VISTALL
 * @since 2026-08-22
 */
public final class DesktopQtNativePaths {
    /**
     * Set by the container startup when the distribution carries a plugin directory of its own.
     */
    public static final String PLUGIN_PATH_PROPERTY = "consulo.desktop.qt.plugin-path";

    private DesktopQtNativePaths() {
    }

    public static void applyBundledPluginPath() {
        String pluginPath = System.getProperty(PLUGIN_PATH_PROPERTY);
        if (pluginPath == null || pluginPath.isEmpty()) {
            return;
        }

        QCoreApplication.setLibraryPaths(List.of(pluginPath));
    }
}
