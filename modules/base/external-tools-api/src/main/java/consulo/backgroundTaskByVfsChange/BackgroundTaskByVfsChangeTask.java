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
package consulo.backgroundTaskByVfsChange;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import consulo.annotation.access.RequiredReadAction;
import consulo.util.pointers.Named;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 22:47/06.10.13
 */
public interface BackgroundTaskByVfsChangeTask extends Named {
  boolean isEnabled();

  void setEnabled(boolean enabled);

  @Nonnull
  String getProviderName();

  @Nullable
  BackgroundTaskByVfsChangeProvider getProvider();

  @Nonnull
  VirtualFilePointer getVirtualFilePointer();

  @Nonnull
  BackgroundTaskByVfsParameters getParameters();

  @Nonnull
  @RequiredReadAction
  String[] getGeneratedFilePaths();

  @Nonnull
  @RequiredReadAction
  VirtualFile[] getGeneratedFiles();

  @Nonnull
  Project getProject();

  @Nonnull
  BackgroundTaskByVfsChangeTask clone();
}
