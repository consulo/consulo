/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.container.boot;

import consulo.container.boot.ContainerPathManager;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * @author VISTALL
 * @since 28/04/2021
 *
 * TODO [VISTALL] dummy - need shared desktop container for it
 */
public class DesktopSwtContainerPathManager extends ContainerPathManager {

  @Nonnull
  @Override
  public String getHomePath() {
    return System.getProperty("consulo.home.path");
  }

  @Nonnull
  @Override
  public File getAppHomeDirectory() {
    return new File(System.getProperty("user.dir"));
  }

  @Nonnull
  @Override
  public String getConfigPath() {
    return new File(getAppHomeDirectory(), "/.sandbox/config").getPath();
  }

  @Nonnull
  @Override
  public String getSystemPath() {
    return new File(getAppHomeDirectory(), "/.sandbox/system").getPath();
  }

  @Nonnull
  @Override
  public File getDocumentsDir() {
    return new File(System.getProperty("user.dir"), "Consulo Projects");
  }
}
