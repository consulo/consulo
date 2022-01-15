/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.remoteServer.configuration.deployment;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * @author nik
 */
public interface DeploymentSource {
  @Nullable
  File getFile();

  @Nullable
  String getFilePath();

  @Nonnull
  String getPresentableName();

  @Nullable
  Image getIcon();

  boolean isValid();

  boolean isArchive();

  @Nonnull
  DeploymentSourceType<?> getType();
}
