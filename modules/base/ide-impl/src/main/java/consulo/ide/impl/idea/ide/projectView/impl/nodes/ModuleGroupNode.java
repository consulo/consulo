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

import consulo.application.AllIcons;
import consulo.ide.IdeBundle;
import consulo.ui.ex.tree.PresentationData;
import consulo.ide.impl.idea.ide.projectView.ProjectViewNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ide.impl.idea.ide.projectView.actions.MoveModulesToGroupAction;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.ide.impl.idea.ide.projectView.impl.ModuleGroup;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.dataContext.DataContext;
import consulo.module.Module;
import consulo.component.ProcessCanceledException;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.annotation.access.RequiredReadAction;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.*;

public abstract class ModuleGroupNode extends ProjectViewNode<ModuleGroup> implements DropTargetNode {
  private static final Logger LOG = Logger.getInstance(ModuleGroupNode.class);

  public ModuleGroupNode(final Project project, final ModuleGroup value, final ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  public ModuleGroupNode(final Project project, final Object value, final ViewSettings viewSettings) {
    this(project, (ModuleGroup)value, viewSettings);
  }

  protected abstract AbstractTreeNode createModuleNode(Module module);

  protected abstract ModuleGroupNode createModuleGroupNode(ModuleGroup moduleGroup);

  @RequiredReadAction
  @Override
  @Nonnull
  public Collection<AbstractTreeNode> getChildren() {
    final Collection<ModuleGroup> childGroups = getValue().childGroups(getProject());
    final List<AbstractTreeNode> result = new ArrayList<>();
    for (final ModuleGroup childGroup : childGroups) {
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
  public Collection<VirtualFile> getRoots() {
    Collection<AbstractTreeNode> children = getChildren();
    Set<VirtualFile> result = new HashSet<>();
    for (AbstractTreeNode each : children) {
      if (each instanceof ProjectViewNode) {
        result.addAll(((ProjectViewNode)each).getRoots());
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
    final String[] groupPath = getValue().getGroupPath();
    presentation.setPresentableText(groupPath[groupPath.length - 1]);
    presentation.setIcon(AllIcons.Nodes.ModuleGroup);
  }

  @Override
  public String getTestPresentation() {
    return "KeymapGroupImpl: " + getValue();
  }

  @Override
  public String getToolTip() {
    return IdeBundle.message("tooltip.module.group");
  }

  @Override
  public int getWeight() {
    return 0;
  }

  @Override
  public int getTypeSortWeight(final boolean sortByType) {
    return 1;
  }

  @Override
  public boolean canDrop(TreeNode[] sourceNodes) {
    final List<Module> modules = extractModules(sourceNodes);
    return !modules.isEmpty();
  }

  @Override
  public void drop(TreeNode[] sourceNodes, DataContext dataContext) {
    final List<Module> modules = extractModules(sourceNodes);
    MoveModulesToGroupAction.doMove(modules.toArray(new Module[modules.size()]), getValue(), dataContext);
  }

  @Override
  public void dropExternalFiles(PsiFileSystemItem[] sourceFileArray, DataContext dataContext) {
    // Do nothing, N/A
  }

  private static List<Module> extractModules(TreeNode[] sourceNodes) {
    final List<Module> modules = new ArrayList<>();
    for (TreeNode sourceNode : sourceNodes) {
      if (sourceNode instanceof DefaultMutableTreeNode) {
        final Object userObject = AbstractProjectViewPane.extractValueFromNode(sourceNode);
        if (userObject instanceof Module) {
          modules.add((Module)userObject);
        }
      }
    }
    return modules;
  }
}
