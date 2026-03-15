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
 * @author VISTALL
 * @since 2019-12-07
 */
class WebContainerPathManager extends ContainerPathManager {
  public static final String CONSULO_PLUGINS_PATHS = "consulo.plugins.paths";

  
  @Override
  public String getHomePath() {
    return System.getProperty("consulo.home.path");
  }

  
  @Override
  public File getAppHomeDirectory() {
    return new File(System.getProperty("user.dir"));
  }

  
  @Override
  public String getConfigPath() {
    return new File(getAppHomeDirectory(), "/.sandbox/config").getPath();
  }

  
  @Override
  public String getSystemPath() {
    return new File(getAppHomeDirectory(), "/.sandbox/system").getPath();
  }

  
  @Override
  public File getDocumentsDir() {
    return new File(System.getProperty("user.dir"), "Consulo Projects");
  }

  
  @Override
  public String[] getPluginsPaths() {
    String pluginsPath = System.getProperty(CONSULO_PLUGINS_PATHS);
    if(pluginsPath != null) {
      return pluginsPath.split(File.pathSeparator);
    }
    return super.getPluginsPaths();
  }
}
