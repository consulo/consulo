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

import consulo.project.ui.view.tree.ViewSettings;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.module.Module;
import consulo.project.Project;

/**
 * @author anna
 * @since 2005-02-22
 */
public class PackageViewModuleGroupNode extends ModuleGroupNode {
    public PackageViewModuleGroupNode(Project project, Object value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    public PackageViewModuleGroupNode(Project project, ModuleGroup value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    protected AbstractTreeNode createModuleNode(Module module) {
        return new PackageViewModuleNode(module.getProject(), module, getSettings());
    }

    @Override
    protected ModuleGroupNode createModuleGroupNode(ModuleGroup moduleGroup) {
        return new PackageViewModuleGroupNode(getProject(), moduleGroup, getSettings());
    }
}
