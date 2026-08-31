/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.internal.startup;

import consulo.container.boot.ContainerPathManager;

import java.io.File;

/**
 * The paths a launcher may point somewhere else, under the same property names the desktop manager answers to.
 * Without them every instance shares one config directory next to the working directory, and a launcher which
 * asks for its own is quietly ignored.
 *
 * @author VISTALL
 * @since 2019-12-07
 */
class WebContainerPathManager extends ContainerPathManager {
    public static final String CONSULO_PLUGINS_PATHS = "consulo.plugins.paths";

    private static final String[] PROPERTY_CONFIG_PATH = {"consulo.config.path", "idea.config.path"};
    private static final String[] PROPERTY_SYSTEM_PATH = {"consulo.system.path", "idea.system.path"};
    private static final String[] PROPERTY_SCRATCH_PATH = {"consulo.scratch.path", "idea.scratch.path"};
    private static final String[] PROPERTY_LOG_PATH = {"consulo.log.path", "idea.log.path"};

    private static final String CONSULO_INSTALL_PLUGINS_PATH = "consulo.install.plugins.path";

    @Override
    public String getHomePath() {
        String homePath = System.getProperty("consulo.home.path");
        if (homePath != null) {
            return homePath;
        }
        homePath = System.getProperty("idea.home.path");
        if (homePath != null) {
            return homePath;
        }
        throw new IllegalArgumentException("HomePath is not set");
    }

    @Override
    public File getAppHomeDirectory() {
        return new File(System.getProperty("user.dir"));
    }

    @Override
    public String getConfigPath() {
        String propertyValue = selectPropertyValue(PROPERTY_CONFIG_PATH);
        if (propertyValue != null) {
            return getAbsolutePath(propertyValue);
        }
        return new File(getAppHomeDirectory(), "/.sandbox/config").getPath();
    }

    @Override
    public String getSystemPath() {
        String propertyValue = selectPropertyValue(PROPERTY_SYSTEM_PATH);
        if (propertyValue != null) {
            return getAbsolutePath(propertyValue);
        }
        return new File(getAppHomeDirectory(), "/.sandbox/system").getPath();
    }

    @Override
    public String getScratchPath() {
        String propertyValue = selectPropertyValue(PROPERTY_SCRATCH_PATH);
        if (propertyValue != null) {
            return getAbsolutePath(propertyValue);
        }
        return super.getScratchPath();
    }

    @Override
    public File getLogPath() {
        String propertyValue = selectPropertyValue(PROPERTY_LOG_PATH);
        if (propertyValue != null) {
            return new File(getAbsolutePath(propertyValue));
        }
        return super.getLogPath();
    }

    @Override
    public File getDocumentsDir() {
        return new File(System.getProperty("user.dir"), "Consulo Projects");
    }

    @Override
    public String getInstallPluginsPath() {
        String propertyValue = System.getProperty(CONSULO_INSTALL_PLUGINS_PATH);
        if (propertyValue != null) {
            return getAbsolutePath(propertyValue);
        }
        return super.getInstallPluginsPath();
    }

    @Override
    public String[] getPluginsPaths() {
        String pluginsPath = selectPropertyValue(CONSULO_PLUGINS_PATHS);
        if (pluginsPath != null) {
            String[] splitPaths = pluginsPath.split(File.pathSeparator);
            for (int i = 0; i < splitPaths.length; i++) {
                splitPaths[i] = getAbsolutePath(splitPaths[i]);
            }
            return splitPaths;
        }
        // the base answers the plugins of the config directory, which is what an overridden config path wants
        return super.getPluginsPaths();
    }

    private static String getAbsolutePath(String path) {
        String value = trimPathQuotes(path);
        if (value.startsWith("~/") || value.startsWith("~\\")) {
            value = System.getProperty("user.home") + value.substring(1);
        }
        return new File(value).getAbsolutePath();
    }

    private static String trimPathQuotes(String path) {
        if (path.length() >= 3 && path.charAt(0) == '"' && path.charAt(path.length() - 1) == '"') {
            return path.substring(1, path.length() - 1);
        }
        return path;
    }

    private static String selectPropertyValue(String... properties) {
        for (String property : properties) {
            String propertyValue = System.getProperty(property);
            if (propertyValue != null && !propertyValue.isEmpty()) {
                return propertyValue;
            }
        }
        return null;
    }
}
