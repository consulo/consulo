/*
 * Copyright 2013-2022 consulo.io
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
package consulo.module.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.content.impl.internal.GlobalLibraryRootListenerProvider;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerListener;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09-Apr-22
 */
@Singleton
@ServiceImpl
public class GlobalLibraryRootListenerProviderImpl implements GlobalLibraryRootListenerProvider {
  private  final VirtualFilePointerListener tellAllProjectsTheirRootsAreGoingToChange = new VirtualFilePointerListener() {
    @Override
    public void beforeValidityChanged(@Nonnull VirtualFilePointer[] pointers) {
      //todo check if this sdk is really used in the project
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
        VirtualFilePointerListener listener = ((ProjectRootManagerImpl)ProjectRootManager.getInstance(project)).getRootsValidityChangedListener();
        listener.beforeValidityChanged(pointers);
      }
    }

    @Override
    public void validityChanged(@Nonnull VirtualFilePointer[] pointers) {
      //todo check if this sdk is really used in the project
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
        VirtualFilePointerListener listener = ((ProjectRootManagerImpl)ProjectRootManager.getInstance(project)).getRootsValidityChangedListener();
        listener.validityChanged(pointers);
      }
    }
  };

  @Nonnull
  @Override
  public VirtualFilePointerListener getListener() {
    return tellAllProjectsTheirRootsAreGoingToChange;
  }
}
