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
package consulo.virtualFileSystem.fileWatcher;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.util.pointer.Named;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 22:47/06.10.13
 */
public interface BackgroundTaskByVfsChangeTask extends Named {
  boolean isEnabled();

  void setEnabled(boolean enabled);

  
  String getProviderName();

  @Nullable BackgroundTaskByVfsChangeProvider getProvider();

  
  VirtualFilePointer getVirtualFilePointer();

  
  BackgroundTaskByVfsParameters getParameters();

  
  @RequiredReadAction
  String[] getGeneratedFilePaths();

  
  @RequiredReadAction
  VirtualFile[] getGeneratedFiles();

  
  Project getProject();

  
  BackgroundTaskByVfsChangeTask clone();
}
