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

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.SystemProperties;
import consulo.annotation.access.RequiredReadAction;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ProjectViewProjectNode extends AbstractProjectNode {

  public ProjectViewProjectNode(Project project, ViewSettings viewSettings) {
    super(project, project, viewSettings);
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Collection<AbstractTreeNode> getChildren() {
    List<VirtualFile> topLevelContentRoots = BaseProjectViewDirectoryHelper.getTopLevelRoots(myProject);

    Set<Module> modules = new LinkedHashSet<Module>(topLevelContentRoots.size());

    Project project = getProject();

    for (VirtualFile root : topLevelContentRoots) {
      final Module module = ModuleUtil.findModuleForFile(root, project);
      if (module != null) { // Some people exclude module's content roots...
        modules.add(module);
      }
    }

    ArrayList<AbstractTreeNode> nodes = new ArrayList<>();
    final PsiManager psiManager = PsiManager.getInstance(project);

    /*
    for (VirtualFile root : reduceRoots(topLevelContentRoots)) {
      nodes.add(new PsiDirectoryNode(getProject(), psiManager.findDirectory(root), getSettings()));
    }
    */

    nodes.addAll(modulesAndGroups(modules.toArray(new Module[modules.size()])));

    final VirtualFile baseDir = project.getBaseDir();
    if (baseDir == null) return nodes;

    final VirtualFile[] files = baseDir.getChildren();
    for (VirtualFile file : files) {
      if (ModuleUtil.findModuleForFile(file, project) == null) {
        if (!file.isDirectory()) {
          PsiFile psiFile = psiManager.findFile(file);
          if(psiFile != null) {
            nodes.add(new PsiFileNode(project, psiFile, getSettings()));
          }
        }
      }
    }

    if (getSettings().isShowLibraryContents()) {
      nodes.add(new ExternalLibrariesNode(project, getSettings()));
    }

    return nodes;
  }

  private static List<VirtualFile> reduceRoots(List<VirtualFile> roots) {
    if (roots.isEmpty()) return Collections.emptyList();

    String userHome;
    try {
      userHome = FileUtil.toSystemIndependentName(new File(SystemProperties.getUserHome()).getCanonicalPath());
    }
    catch (IOException e) {
      userHome = null;
    }

    Collections.sort(roots, (o1, o2) -> o1.getPath().compareTo(o2.getPath()));

    Iterator<VirtualFile> it = roots.iterator();
    VirtualFile current = it.next();

    List<VirtualFile> reducedRoots = new ArrayList<VirtualFile>();
    while (it.hasNext()) {
      VirtualFile next = it.next();
      VirtualFile common = VfsUtil.getCommonAncestor(current, next);

      if (common == null || common.getParent() == null || Comparing.equal(common.getPath(), userHome)) {
        reducedRoots.add(current);
        current = next;
      }
      else {
        current = common;
      }
    }

    reducedRoots.add(current);
    return reducedRoots;
  }

  @Override
  protected AbstractTreeNode createModuleGroup(final Module module) {
    final VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
    if (roots.length == 1) {
      final PsiDirectory psi = PsiManager.getInstance(myProject).findDirectory(roots[0]);
      if (psi != null) {
        return new PsiDirectoryNode(myProject, psi, getSettings());
      }
    }

    return new ProjectViewModuleNode(getProject(), module, getSettings());
  }

  @Override
  protected AbstractTreeNode createModuleGroupNode(final ModuleGroup moduleGroup) {
    return new ProjectViewModuleGroupNode(getProject(), moduleGroup, getSettings());
  }
}
