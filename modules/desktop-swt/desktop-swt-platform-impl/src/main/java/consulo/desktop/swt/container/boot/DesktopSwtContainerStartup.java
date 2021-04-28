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
import consulo.container.boot.ContainerStartup;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author VISTALL
 * @since 27/04/2021
 */
public class DesktopSwtContainerStartup implements ContainerStartup {
  @Nonnull
  @Override
  public ContainerPathManager createPathManager(@Nonnull Map<String, Object> args) {
    return new DesktopSwtContainerPathManager();
  }

  @Override
  public void run(@Nonnull Map<String, Object> args) {

  }

  @Override
  public void destroy() {

  }
}
