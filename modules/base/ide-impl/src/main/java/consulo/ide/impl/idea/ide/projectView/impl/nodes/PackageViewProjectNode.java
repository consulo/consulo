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
package consulo.ide.impl.idea.ide.projectView.impl.nodes;

import consulo.project.ui.view.tree.BaseProjectViewDirectoryHelper;
import consulo.project.ui.view.tree.PackageNodeUtil;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.annotation.access.RequiredReadAction;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.PsiPackageManager;
import consulo.language.content.LanguageContentFolderScopes;

import jakarta.annotation.Nonnull;
import java.util.*;

public class PackageViewProjectNode extends AbstractProjectNode {
  public PackageViewProjectNode(Project project, ViewSettings viewSettings) {
    super(project, project, viewSettings);
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Collection<AbstractTreeNode> getChildren() {
    if (getSettings().isShowModules()) {
      final List<Module> allModules = new ArrayList<Module>(Arrays.asList(ModuleManager.getInstance(getProject()).getModules()));
      for (Iterator<Module> it = allModules.iterator(); it.hasNext(); ) {
        final Module module = it.next();
        final VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getContentFolderFiles(LanguageContentFolderScopes.productionAndTest());
        if (sourceRoots.length == 0) {
          // do not show modules with no source roots configured
          it.remove();
        }
      }
      return modulesAndGroups(allModules.toArray(new Module[allModules.size()]));
    }
    else {
      final List<VirtualFile> sourceRoots = new ArrayList<VirtualFile>();
      final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(myProject);
      ContainerUtil.addAll(sourceRoots, projectRootManager.getContentSourceRoots());

      final PsiManager psiManager = PsiManager.getInstance(myProject);
      final List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
      final Set<PsiPackage> topLevelPackages = new HashSet<PsiPackage>();

      for (final VirtualFile root : sourceRoots) {
        final PsiDirectory directory = psiManager.findDirectory(root);
        if (directory == null) {
          continue;
        }
        final PsiPackage directoryPackage = PsiPackageManager.getInstance(myProject).findAnyPackage(directory);
        if (directoryPackage == null || PackageNodeUtil.isPackageDefault(directoryPackage)) {
          // add subpackages
          final PsiDirectory[] subdirectories = directory.getSubdirectories();
          for (PsiDirectory subdirectory : subdirectories) {
            final PsiPackage aPackage = PsiPackageManager.getInstance(myProject).findAnyPackage(subdirectory);
            if (aPackage != null && !PackageNodeUtil.isPackageDefault(aPackage)) {
              topLevelPackages.add(aPackage);
            }
          }
          // add non-dir items
          children.addAll(BaseProjectViewDirectoryHelper.getDirectoryChildren(directory, getSettings(), false));
        }
        else {
          // this is the case when a source root has pakage prefix assigned
          topLevelPackages.add(directoryPackage);
        }
      }

      for (final PsiPackage psiPackage : topLevelPackages) {
        PackageNodeUtil.addPackageAsChild(children, psiPackage, null, getSettings(), false);
      }

      if (getSettings().isShowLibraryContents()) {
        children.add(new PackageViewLibrariesNode(getProject(), null, getSettings()));
      }

      return children;
    }
  }

  @Override
  protected AbstractTreeNode createModuleGroup(final Module module) {
    return new PackageViewModuleNode(getProject(), module, getSettings());
  }

  @Override
  protected AbstractTreeNode createModuleGroupNode(final ModuleGroup moduleGroup) {
    return new PackageViewModuleGroupNode(getProject(), moduleGroup, getSettings());
  }

  @Override
  public boolean someChildContainsFile(VirtualFile file) {
    return true;
  }
}
