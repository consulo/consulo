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
import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;

/**
 * @author anna
 * @since 2005-02-22
 */
public class ProjectViewModuleGroupNode extends ModuleGroupNode {
    public ProjectViewModuleGroupNode(Project project, Object value, ViewSettings viewSettings) {
        super(project, (ModuleGroup) value, viewSettings);
    }

    public ProjectViewModuleGroupNode(Project project, ModuleGroup value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    @RequiredReadAction
    protected AbstractTreeNode createModuleNode(Module module) {
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
    protected ModuleGroupNode createModuleGroupNode(ModuleGroup moduleGroup) {
        return new ProjectViewModuleGroupNode(getProject(), moduleGroup, getSettings());
    }
}
