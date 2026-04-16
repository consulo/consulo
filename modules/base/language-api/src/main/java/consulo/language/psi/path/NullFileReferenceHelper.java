/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.psi.path;

import consulo.annotation.ReviewAfterIssueFix;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

@ExtensionImpl(order = "last")
public class NullFileReferenceHelper extends FileReferenceHelper {
  public static final NullFileReferenceHelper INSTANCE = new NullFileReferenceHelper();

  @Override
  public @Nullable PsiFileSystemItem findRoot(Project project, VirtualFile file) {
    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    VirtualFile contentRootForFile = index.getContentRootForFile(file);

    return contentRootForFile != null ? PsiManager.getInstance(project).findDirectory(contentRootForFile) : null;
  }

  @Override
  @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1504", todo = "Remove NullAway suppression")
  @SuppressWarnings("NullAway")
  public Collection<PsiFileSystemItem> getRoots(Module module) {
    return ContainerUtil.mapNotNull(
      ModuleRootManager.getInstance(module).getContentRoots(),
        virtualFile -> PsiManager.getInstance(module.getProject()).findDirectory(virtualFile)
    );
  }

  @Override
  public Collection<PsiFileSystemItem> getContexts(Project project, VirtualFile file) {
    PsiFileSystemItem item = getPsiFileSystemItem(project, file);
    if (item != null) {
      PsiFileSystemItem parent = item.getParent();
      if (parent != null) {
        return Collections.singleton(parent);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public boolean isMine(Project project, VirtualFile file) {
    return ProjectRootManager.getInstance(project).getFileIndex().isInContent(file);
  }

  @Override
  public boolean isFallback() {
    return true;
  }
}
