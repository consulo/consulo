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

import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.annotation.access.RequiredReadAction;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StructureViewModuleNode extends AbstractModuleNode {
    public StructureViewModuleNode(Project project, Module value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Collection<AbstractTreeNode> getChildren() {
        final Module module = getValue();
        if (module == null) {
            // just deleted a module from project view
            return Collections.emptyList();
        }
        final List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>(2);
        children.add(new LibraryGroupNode(getProject(), new LibraryGroupElement(module), getSettings()) {
            @Override
            public boolean isAlwaysExpand() {
                return true;
            }
        });

        children.add(new ModuleListNode(getProject(), module, getSettings()));
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

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
        return false;
    }

    @Override
    public boolean someChildContainsFile(VirtualFile file) {
        return true;
    }
}
