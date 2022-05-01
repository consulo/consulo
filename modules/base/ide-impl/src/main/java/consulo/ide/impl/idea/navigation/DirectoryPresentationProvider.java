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
package consulo.ide.impl.idea.navigation;

import consulo.application.AllIcons;
import consulo.ui.ex.tree.PresentationData;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectRootsUtil;
import consulo.application.Application;
import consulo.module.Module;
import consulo.navigation.ItemPresentation;
import consulo.navigation.ItemPresentationProvider;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.content.ContentFolderTypeProvider;

public class DirectoryPresentationProvider implements ItemPresentationProvider<PsiDirectory> {
  @Override
  public ItemPresentation getPresentation(final PsiDirectory directory) {
    final VirtualFile vFile = directory.getVirtualFile();
    final Project project = directory.getProject();
    final String locationString = vFile.getPath();

    if (ProjectRootsUtil.isProjectHome(directory)) {
      return new PresentationData(project.getName(), locationString, Application.get().getIcon(), null);
    }

    if (ProjectRootsUtil.isModuleContentRoot(directory)) {
      final Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(vFile);
      assert module != null : directory;
      return new PresentationData(module.getName(), locationString, AllIcons.Nodes.Module, null);
    }

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    ContentFolderTypeProvider contentFolderTypeForFile = fileIndex.getContentFolderTypeForFile(vFile);
    if(contentFolderTypeForFile != null) {
      return new PresentationData(directory.getName(), locationString, contentFolderTypeForFile.getIcon(), null);
    }
    return new PresentationData(directory.getName(), locationString, AllIcons.Nodes.TreeClosed, null);
  }
}
