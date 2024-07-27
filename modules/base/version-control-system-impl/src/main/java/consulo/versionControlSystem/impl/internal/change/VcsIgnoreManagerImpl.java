/*
 * Copyright 2013-2024 consulo.io
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.IgnoredFileProvider;
import consulo.versionControlSystem.change.VcsIgnoreManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 27-Jul-24
 */
@ServiceImpl
@Singleton
public class VcsIgnoreManagerImpl implements VcsIgnoreManager {
  private final Project myProject;

  @Inject
  public VcsIgnoreManagerImpl(Project project) {
    myProject = project;
  }

  @Override
  public boolean isPotentiallyIgnoredFile(@Nonnull FilePath filePath) {
    return ReadAction.compute(() -> {
      return myProject.getExtensionPoint(IgnoredFileProvider.class).findFirstSafe(it -> it.isIgnoredFilePath(filePath)) != null;
    });
  }
}
