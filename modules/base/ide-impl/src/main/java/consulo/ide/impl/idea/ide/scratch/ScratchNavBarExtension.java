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
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.navigationToolbar.AbstractNavBarModelExtension;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.scratch.RootType;
import consulo.language.scratch.ScratchFileService;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

@ExtensionImpl
public class ScratchNavBarExtension extends AbstractNavBarModelExtension {
  @Nullable
  @Override
  public String getPresentableText(Object object) {
    if (!(object instanceof PsiElement)) return null;
    Project project = ((PsiElement)object).getProject();
    VirtualFile virtualFile = PsiUtilCore.getVirtualFile((PsiElement)object);
    if (virtualFile == null || !virtualFile.isValid()) return null;
    RootType rootType = ScratchFileService.getInstance().getRootType(virtualFile);
    if (rootType == null) return null;
    if (virtualFile.isDirectory() && additionalRoots(project).contains(virtualFile)) {
      return rootType.getDisplayName();
    }
    return rootType.substituteName(project, virtualFile);
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> additionalRoots(Project project) {
    Set<VirtualFile> result = ContainerUtil.newLinkedHashSet();
    LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    ScratchFileService app = ScratchFileService.getInstance();
    for (RootType r : RootType.getAllRootTypes()) {
      ContainerUtil.addIfNotNull(result, fileSystem.findFileByPath(app.getRootPath(r)));
    }
    return result;
  }
}
