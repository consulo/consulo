/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.checkout;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.versionControlSystem.VcsKey;

import java.io.File;
import java.util.Collections;

@ExtensionImpl
public class RegisterMappingCheckoutListener implements VcsAwareCheckoutListener {
  @Override
  public boolean processCheckedOutDirectory(Project currentProject, File directory, VcsKey vcsKey) {
    Project project = CompositeCheckoutListener.findProjectByBaseDirLocation(directory);
    if (project != null) {
      ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
      if (!vcsManager.hasAnyMappings()) {
        vcsManager.setDirectoryMappings(Collections.singletonList(VcsDirectoryMapping.createDefault(vcsKey.getName())));
      }
      return true;
    }
    return false;
  }
}
