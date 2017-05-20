/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.projectImport;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotations.DeprecationInfo;
import consulo.annotations.RequiredDispatchThread;
import consulo.project.ProjectOpenProcessors;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

public abstract class ProjectOpenProcessor {
  @Deprecated
  @DeprecationInfo("Not Used")
  public static final ExtensionPointName<ProjectOpenProcessor> EP_NAME = new ExtensionPointName<>("com.intellij.projectOpenProcessor");

  @Nullable
  @Deprecated
  public static ProjectOpenProcessor findProcessor(VirtualFile file) {
    return ProjectOpenProcessors.getInstance().findProcessor(file);
  }

  public abstract String getName();

  @NotNull
  @Language("HTML")
  public String getFileSample() {
    return getName();
  }

  @NotNull
  public abstract Icon getIcon();

  @Nullable
  public Icon getIcon(final VirtualFile file) {
    return getIcon();
  }

  public abstract boolean canOpenProject(VirtualFile file);

  @Deprecated
  @DeprecationInfo("Not used")
  public boolean lookForProjectsInDirectory() {
    return true;
  }

  @Deprecated
  @DeprecationInfo("Not used")
  public boolean isProjectFile(VirtualFile file) {
    return canOpenProject(file);
  }

  @Nullable
  @RequiredDispatchThread
  public abstract Project doOpenProject(@NotNull VirtualFile virtualFile, @Nullable Project projectToClose, boolean forceOpenInNewFrame);

  public void refreshProjectFiles(File basedir) {

  }
}
