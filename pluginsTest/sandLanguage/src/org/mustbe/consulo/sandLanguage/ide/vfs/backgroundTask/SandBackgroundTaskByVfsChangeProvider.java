/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.sandLanguage.ide.vfs.backgroundTask;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeProvider;
import org.consulo.vfs.backgroundTask.BackgroundTaskByVfsParameters;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.sandLanguage.lang.SandFileType;

/**
 * @author VISTALL
 * @since 30.04.14
 */
public class SandBackgroundTaskByVfsChangeProvider extends BackgroundTaskByVfsChangeProvider.ByFileType {
  public SandBackgroundTaskByVfsChangeProvider() {
    super(SandFileType.INSTANCE);
  }

  @Override
  public void setDefaultParameters(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull BackgroundTaskByVfsParameters parameters) {
    parameters.setExePath("notepad.exe");
    parameters.setProgramParameters(virtualFile.getPresentableUrl());
    parameters.setWorkingDirectory("$FileParentPath$");
  }

  @NotNull
  @Override
  public String getTemplateName() {
    return "#Sand";
  }
}
