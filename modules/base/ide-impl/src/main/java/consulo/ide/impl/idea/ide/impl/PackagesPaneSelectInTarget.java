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
package consulo.ide.impl.idea.ide.impl;

import consulo.ide.impl.idea.ide.projectView.impl.PackageViewPane;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiFileSystemItem;
import consulo.localize.LocalizeValue;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.StandardTargetWeights;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.virtualFileSystem.VirtualFile;

public class PackagesPaneSelectInTarget extends ProjectViewSelectInTarget {
  public PackagesPaneSelectInTarget(Project project) {
    super(project);
  }

  @Override
  public LocalizeValue getActionText() {
    return ProjectUIViewLocalize.selectInPackages();
  }

  @Override
  public boolean canSelect(PsiFileSystemItem file) {
    if (!super.canSelect(file)) return false;
    final VirtualFile vFile = PsiUtilBase.getVirtualFile(file);

    return canSelect(vFile);
  }

  private boolean canSelect(final VirtualFile vFile) {
    if (vFile != null && vFile.isValid()) {
      final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
      return fileIndex.isInSourceContent(vFile) || isInLibraryContentOnly(vFile);
    }
    else {
      return false;
    }
  }

  @Override
  public boolean isSubIdSelectable(String subId, SelectInContext context) {
    return canSelect(context);
  }

  private boolean isInLibraryContentOnly(final VirtualFile vFile) {
    if (vFile == null) {
      return false;
    }
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    return (projectFileIndex.isInLibraryClasses(vFile) || projectFileIndex.isInLibrarySource(vFile)) && !projectFileIndex.isInSourceContent(vFile);
  }

  @Override
  public String getMinorViewId() {
    return PackageViewPane.ID;
  }

  @Override
  public float getWeight() {
    return StandardTargetWeights.PACKAGES_WEIGHT;
  }

  @Override
  protected boolean canWorkWithCustomObjects() {
    return false;
  }
}
