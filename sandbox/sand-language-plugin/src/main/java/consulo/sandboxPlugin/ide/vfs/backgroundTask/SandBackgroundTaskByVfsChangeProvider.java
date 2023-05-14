/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.vfs.backgroundTask;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeProvider;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsParameters;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 30.04.14
 */
@ExtensionImpl
public class SandBackgroundTaskByVfsChangeProvider extends BackgroundTaskByVfsChangeProvider.ByFileType {
  public SandBackgroundTaskByVfsChangeProvider() {
    super(SandFileType.INSTANCE);
  }

  @Override
  public void setDefaultParameters(@Nonnull Project project, @Nonnull VirtualFile virtualFile, @Nonnull BackgroundTaskByVfsParameters parameters) {
    parameters.setExePath("notepad.exe");
    parameters.setProgramParameters(virtualFile.getPresentableUrl());
    parameters.setWorkingDirectory("$FileParentPath$");
  }

  @Nonnull
  @Override
  public String getTemplateName() {
    return "#Sand";
  }
}
