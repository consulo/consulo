/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 18-Aug-17
 *
 * from kotlin platform\projectModel-impl\src\com\intellij\project\project.kt
 */
public class ProjectKt {
  public static boolean isDirectoryBased(@Nonnull Project project) {
    if(project.isDefault()) {
      return false;
    }
    return true;
  }

  @Nullable
  public static VirtualFile getDirectoryStoreFile(Project project) {
    VirtualFile baseDir = project.getBaseDir();
    if(baseDir == null) {
      return null;
    }
    return baseDir.findChild(Project.DIRECTORY_STORE_FOLDER);
  }
}
