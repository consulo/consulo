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
import consulo.component.ProcessCanceledException;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewPaneImpl;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.*;

public abstract class AbstractProjectNode extends ProjectViewNode<Project> {
    private static final Logger LOG = Logger.getInstance(AbstractProjectNode.class);

    protected AbstractProjectNode(Project project, Project value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @RequiredReadAction
    protected Collection<AbstractTreeNode> modulesAndGroups(Module[] modules) {
        Map<String, List<Module>> groups = new HashMap<>();
        List<Module> nonGroupedModules = new ArrayList<>(Arrays.asList(modules));
        for (Module module : modules) {
            String[] path = ModuleManager.getInstance(getProject()).getModuleGroupPath(module);
            if (path != null) {
                String topLevelGroupName = path[0];
                List<Module> moduleList = groups.get(topLevelGroupName);
                if (moduleList == null) {
                    moduleList = new ArrayList<>();
                    groups.put(topLevelGroupName, moduleList);
                }
                moduleList.add(module);
                nonGroupedModules.remove(module);
            }
        }
        List<AbstractTreeNode> result = new ArrayList<>();
        try {
            for (String groupPath : groups.keySet()) {
                result.add(createModuleGroupNode(new ModuleGroup(new String[]{groupPath})));
            }
            for (Module module : nonGroupedModules) {
                result.add(createModuleGroup(module));
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Exception e) {
            LOG.error(e);
            return new ArrayList<>();
        }
        return result;
    }

    protected abstract AbstractTreeNode createModuleGroup(Module module);

    protected abstract AbstractTreeNode createModuleGroupNode(ModuleGroup moduleGroup);

    @Override
    public void update(PresentationData presentation) {
        presentation.setIcon(getProject().getApplication().getIcon());
        presentation.setPresentableText(getProject().getName());
    }

    @Override
    public String getTestPresentation() {
        return "Project";
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
        return ProjectViewPaneImpl.canBeSelectedInProjectView(getProject(), file);
    }
}
