/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide;

import consulo.project.Project;
import consulo.project.ui.view.SelectInContext;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class FileSelectInContext implements SelectInContext {
  private final Project myProject;
  private final VirtualFile myFile;

  public FileSelectInContext(@Nonnull Project project, @Nonnull VirtualFile file) {
    myProject = project;
    myFile = file;
  }

  @Nonnull
  @Override
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  @Override
  public VirtualFile getVirtualFile() {
    return myFile;
  }

  @Override
  public Object getSelectorInFile() {
    return null;
  }
}
