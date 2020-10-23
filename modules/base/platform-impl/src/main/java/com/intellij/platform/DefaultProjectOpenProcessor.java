/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.platform;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectOpenProcessor;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * @author max
 */
public class DefaultProjectOpenProcessor extends ProjectOpenProcessor {
  private static final DefaultProjectOpenProcessor INSTANCE = new DefaultProjectOpenProcessor();

  public static DefaultProjectOpenProcessor getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean canOpenProject(@Nonnull File file) {
    return file.isDirectory() && new File(file, Project.DIRECTORY_STORE_FOLDER + "/modules.xml").exists();
  }

  @Override
  @Nonnull
  public Image getIcon(@Nonnull VirtualFile file) {
    return Application.get().getIcon();
  }

  @Nonnull
  @Override
  public String getFileSample() {
    return "<b>Consulo</b> project";
  }

  @Nonnull
  @Override
  public AsyncResult<Project> doOpenProjectAsync(@Nonnull VirtualFile baseDir, @Nonnull UIAccess uiAccess) {
    return ProjectManager.getInstance().openProjectAsync(baseDir, uiAccess);
  }
}
