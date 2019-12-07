/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.openapi.application;

import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * from kotlin
 */
public class PathManagerEx {
  private static final Logger LOGGER = Logger.getInstance(PathManagerEx.class);

  @Nonnull
  public static Path getAppSystemDir() {
    Path path = Paths.get(ContainerPathManager.get().getSystemPath());
    try {
      return path.toRealPath();
    }
    catch (IOException e) {
      LOGGER.error(e);
    }
    return path;
  }
}
