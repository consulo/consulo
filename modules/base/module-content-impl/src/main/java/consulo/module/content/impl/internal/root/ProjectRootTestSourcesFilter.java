/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.module.content.impl.internal.root;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.project.content.TestSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class ProjectRootTestSourcesFilter extends TestSourcesFilter {
  @Override
  public boolean isTestSource(@Nonnull VirtualFile file, @Nonnull Project project) {
    return ProjectFileIndex.getInstance(project).isInTestSourceContent(file);
  }
}
