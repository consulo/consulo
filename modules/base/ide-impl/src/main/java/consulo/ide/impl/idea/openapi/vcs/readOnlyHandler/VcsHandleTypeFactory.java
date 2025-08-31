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
package consulo.ide.impl.idea.openapi.vcs.readOnlyHandler;

import consulo.annotation.component.ExtensionImpl;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@ExtensionImpl
public class VcsHandleTypeFactory implements HandleTypeFactory {
  private final Project myProject;

  @Inject
  public VcsHandleTypeFactory(Project project) {
    myProject = project;
  }

  @Override
  @Nullable
  public HandleType createHandleType(VirtualFile file) {
    if (!myProject.isInitialized()) return null;
    AbstractVcs vcs = ProjectLevelVcsManager.getInstance(myProject).getVcsFor(file);
    if (vcs != null) {
      boolean fileExistsInVcs = vcs.fileExistsInVcs(new FilePathImpl(file));
      if (fileExistsInVcs && vcs.getEditFileProvider() != null) {
        return new VcsHandleType(vcs);
      }
    }
    return null;
  }
}
