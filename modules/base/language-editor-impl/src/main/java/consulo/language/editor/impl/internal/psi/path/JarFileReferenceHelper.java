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

package consulo.language.editor.impl.internal.psi.path;

import consulo.annotation.access.RequiredReadAction;import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.path.FileReferenceHelper;
import consulo.module.Module;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class JarFileReferenceHelper extends FileReferenceHelper {
  @Override
  @RequiredReadAction
  public PsiFileSystemItem getPsiFileSystemItem(Project project, @Nonnull VirtualFile file) {
    return null;
  }

  @Override
  @Nonnull
  public Collection<PsiFileSystemItem> getRoots(@Nonnull Module module) {
    return PsiFileReferenceHelper.getContextsForModule(module, "", null);
  }

  @Override
  @Nonnull
  public Collection<PsiFileSystemItem> getContexts(Project project, @Nonnull VirtualFile file) {
    return Collections.emptyList();
  }

  @Override
  public boolean isMine(Project project, @Nonnull VirtualFile file) {
    return ProjectRootManager.getInstance(project).getFileIndex().isInLibraryClasses(file);
  }
}
