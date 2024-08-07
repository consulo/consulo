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
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.change.ChangesUtil;

import jakarta.annotation.Nonnull;

import java.io.File;

public class IgnoredPathPresentation {
  private final Project myProject;

  public IgnoredPathPresentation(Project project) {
    myProject = project;
  }

  public String alwaysRelative(@Nonnull final String path) {
    final File file = new File(path);
    String relativePath = path;
    if (file.isAbsolute()) {
      relativePath = ChangesUtil.getProjectRelativePath(myProject, file);
      if (relativePath == null) {
        relativePath = path;
      }
    }
    return FileUtil.toSystemIndependentName(relativePath);
  }
}
