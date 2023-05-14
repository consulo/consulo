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
package consulo.virtualFileSystem.fileWatcher.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeManager;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeTask;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 04.03.14
 */
@ExtensionImpl
public class BackgroundTaskByVfsGeneratedSourcesFilter implements GeneratedSourcesFilter {
  private final BackgroundTaskByVfsChangeManager myBackgroundTaskByVfsChangeManager;

  @Inject
  public BackgroundTaskByVfsGeneratedSourcesFilter(BackgroundTaskByVfsChangeManager backgroundTaskByVfsChangeManager) {
    myBackgroundTaskByVfsChangeManager = backgroundTaskByVfsChangeManager;
  }

  @RequiredReadAction
  @Override
  public boolean isGeneratedSource(@Nonnull VirtualFile file) {
    List<BackgroundTaskByVfsChangeTaskImpl> tasksImpl =
      ((BackgroundTaskByVfsChangeManagerImpl)myBackgroundTaskByVfsChangeManager).getTasksImpl();
    if (tasksImpl.isEmpty()) {
      return false;
    }

    for (BackgroundTaskByVfsChangeTask o : tasksImpl) {
      for (VirtualFile virtualFile : o.getGeneratedFiles()) {
        if (virtualFile.equals(file)) {
          return true;
        }
      }
    }
    return false;
  }
}
