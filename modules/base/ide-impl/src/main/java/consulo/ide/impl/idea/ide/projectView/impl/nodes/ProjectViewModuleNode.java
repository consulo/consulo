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

import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.project.ui.view.tree.PsiFileNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.ModuleFileIndex;
import consulo.module.content.ModuleRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.annotation.access.RequiredReadAction;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProjectViewModuleNode extends AbstractModuleNode {
    public ProjectViewModuleNode(Project project, Module value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    @Nonnull
    @RequiredReadAction
    public Collection<AbstractTreeNode> getChildren() {
        Module module = getValue();
        if (module == null || module.isDisposed()) {
            return Collections.emptyList();
        }
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        ModuleFileIndex moduleFileIndex = rootManager.getFileIndex();

        final VirtualFile[] contentRoots = rootManager.getContentRoots();
        final List<AbstractTreeNode> children = new ArrayList<>(contentRoots.length + 1);
        final PsiManager psiManager = PsiManager.getInstance(module.getProject());
        for (final VirtualFile contentRoot : contentRoots) {
            if (!moduleFileIndex.isInContent(contentRoot)) {
                continue;
            }

            if (contentRoot.isDirectory()) {
                PsiDirectory directory = psiManager.findDirectory(contentRoot);
                if (directory != null) {
                    children.add(new PsiDirectoryNode(getProject(), directory, getSettings()));
                }
            }
            else {
                PsiFile file = psiManager.findFile(contentRoot);
                if (file != null) {
                    children.add(new PsiFileNode(getProject(), file, getSettings()));
                }
            }
        }
        return children;
    }

    @Override
    public int getWeight() {
        return 10;
    }

    @Override
    public int getTypeSortWeight(final boolean sortByType) {
        return 2;
    }
}
