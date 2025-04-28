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

import consulo.project.ui.view.tree.ViewSettings;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectTreeStructure;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewPaneImpl;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.StructureViewModuleNode;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.module.Module;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class ModuleStructurePane extends ProjectViewPaneImpl {
    private final Module myModule;

    public ModuleStructurePane(Module module) {
        super(module.getProject());
        myModule = module;
    }

    @Nonnull
    @Override
    public ProjectAbstractTreeStructureBase createStructure() {
        return new ProjectTreeStructure(myProject, ID) {
            @Override
            protected AbstractTreeNode createRoot(Project project, ViewSettings settings) {
                return new StructureViewModuleNode(project, myModule, settings);
            }

            @Override
            public boolean isToBuildChildrenInBackground(@Nonnull Object element) {
                return false;
            }
        };
    }
}
