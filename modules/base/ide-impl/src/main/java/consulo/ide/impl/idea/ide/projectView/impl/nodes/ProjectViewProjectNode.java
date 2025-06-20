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

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.util.ModuleContentUtil;
import consulo.project.Project;
import consulo.project.ui.view.tree.*;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.*;

public class ProjectViewProjectNode extends AbstractProjectNode {
    public ProjectViewProjectNode(Project project, ViewSettings viewSettings) {
        super(project, project, viewSettings);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Collection<AbstractTreeNode> getChildren() {
        List<VirtualFile> topLevelContentRoots = BaseProjectViewDirectoryHelper.getTopLevelRoots(myProject);

        Set<Module> modules = new LinkedHashSet<>(topLevelContentRoots.size());

        Project project = getProject();

        for (VirtualFile root : topLevelContentRoots) {
            Module module = ModuleContentUtil.findModuleForFile(root, project);
            if (module != null) { // Some people exclude module's content roots...
                modules.add(module);
            }
        }

        ArrayList<AbstractTreeNode> nodes = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);

        nodes.addAll(modulesAndGroups(modules.toArray(new Module[modules.size()])));

        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return nodes;
        }

        VirtualFile[] files = baseDir.getChildren();
        for (VirtualFile file : files) {
            if (ModuleContentUtil.findModuleForFile(file, project) == null && !file.isDirectory()) {
                PsiFile psiFile = psiManager.findFile(file);
                if (psiFile != null) {
                    nodes.add(new PsiFileNode(project, psiFile, getSettings()));
                }
            }
        }

        if (getSettings().isShowLibraryContents()) {
            nodes.add(new ExternalLibrariesNode(project, getSettings()));
        }

        return nodes;
    }

    @Override
    @RequiredReadAction
    protected AbstractTreeNode createModuleGroup(Module module) {
        VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        if (roots.length == 1) {
            PsiDirectory psi = PsiManager.getInstance(myProject).findDirectory(roots[0]);
            if (psi != null) {
                return new PsiDirectoryNode(myProject, psi, getSettings());
            }
        }

        return new ProjectViewModuleNode(getProject(), module, getSettings());
    }

    @Override
    protected AbstractTreeNode createModuleGroupNode(ModuleGroup moduleGroup) {
        return new ProjectViewModuleGroupNode(getProject(), moduleGroup, getSettings());
    }
}
