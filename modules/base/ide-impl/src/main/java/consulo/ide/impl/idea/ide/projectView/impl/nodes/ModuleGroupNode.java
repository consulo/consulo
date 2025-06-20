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
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.projectView.actions.MoveModulesToGroupAction;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiFileSystemItem;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.*;

public abstract class ModuleGroupNode extends ProjectViewNode<ModuleGroup> implements DropTargetNode {
    private static final Logger LOG = Logger.getInstance(ModuleGroupNode.class);

    public ModuleGroupNode(Project project, ModuleGroup value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    public ModuleGroupNode(Project project, Object value, ViewSettings viewSettings) {
        this(project, (ModuleGroup) value, viewSettings);
    }

    protected abstract AbstractTreeNode createModuleNode(Module module);

    protected abstract ModuleGroupNode createModuleGroupNode(ModuleGroup moduleGroup);

    @RequiredReadAction
    @Override
    @Nonnull
    public Collection<AbstractTreeNode> getChildren() {
        Collection<ModuleGroup> childGroups = getValue().childGroups(getProject());
        List<AbstractTreeNode> result = new ArrayList<>();
        for (ModuleGroup childGroup : childGroups) {
            result.add(createModuleGroupNode(childGroup));
        }
        Collection<Module> modules = getValue().modulesInGroup(getProject(), false);
        try {
            for (Module module : modules) {
                result.add(createModuleNode(module));
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Exception e) {
            LOG.error(e);
        }

        return result;
    }

    @Override
    @RequiredReadAction
    public Collection<VirtualFile> getRoots() {
        Collection<AbstractTreeNode> children = getChildren();
        Set<VirtualFile> result = new HashSet<>();
        for (AbstractTreeNode each : children) {
            if (each instanceof ProjectViewNode projectViewNode) {
                result.addAll(projectViewNode.getRoots());
            }
        }

        return result;
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
        return someChildContainsFile(file, false);
    }

    @Override
    public void update(PresentationData presentation) {
        String[] groupPath = getValue().getGroupPath();
        presentation.setPresentableText(groupPath[groupPath.length - 1]);
        presentation.setIcon(PlatformIconGroup.nodesModulegroup());
    }

    @Override
    public String getTestPresentation() {
        return "Group: " + getValue();
    }

    @Override
    public String getToolTip() {
        return IdeLocalize.tooltipModuleGroup().get();
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public int getTypeSortWeight(boolean sortByType) {
        return 1;
    }

    @Override
    public boolean canDrop(@Nonnull TreeNode[] sourceNodes) {
        List<Module> modules = extractModules(sourceNodes);
        return !modules.isEmpty();
    }

    @Override
    public void drop(@Nonnull TreeNode[] sourceNodes, @Nonnull DataContext dataContext) {
        List<Module> modules = extractModules(sourceNodes);
        MoveModulesToGroupAction.doMove(modules.toArray(new Module[modules.size()]), getValue(), dataContext);
    }

    @Override
    public void dropExternalFiles(PsiFileSystemItem[] sourceFileArray, DataContext dataContext) {
        // Do nothing, N/A
    }

    private static List<Module> extractModules(TreeNode[] sourceNodes) {
        List<Module> modules = new ArrayList<>();
        for (TreeNode sourceNode : sourceNodes) {
            if (sourceNode instanceof DefaultMutableTreeNode
                && AbstractProjectViewPane.extractValueFromNode(sourceNode) instanceof Module module) {
                modules.add(module);
            }
        }
        return modules;
    }
}
