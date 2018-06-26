/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/**
 * @author cdr
 */
package com.intellij.ide.projectView.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.impl.ProjectPaneSelectInTarget;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.BaseProjectViewDirectoryHelper;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.KeyWithDefaultValue;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.swing.tree.DefaultTreeModel;

public class ProjectViewPane extends AbstractProjectViewPSIPane {
  @NonNls
  public static final String ID = "ProjectPane";

  public ProjectViewPane(Project project) {
    super(project);
  }

  @Override
  public String getTitle() {
    return IdeBundle.message("title.project");
  }

  @Override
  @Nonnull
  public String getId() {
    return ID;
  }

  @Override
  public consulo.ui.image.Image getIcon() {
    return AllIcons.General.ProjectTab;
  }

  @Override
  public SelectInTarget createSelectInTarget() {
    return new ProjectPaneSelectInTarget(myProject);
  }

  @Override
  protected AbstractTreeUpdater createTreeUpdater(AbstractTreeBuilder treeBuilder) {
    return new ProjectViewTreeUpdater(treeBuilder);
  }

  @Override
  public ProjectAbstractTreeStructureBase createStructure() {
    return new ProjectViewPaneTreeStructure();
  }

  @Override
  protected ProjectViewTree createTree(DefaultTreeModel treeModel) {
    return new ProjectViewTree(myProject, treeModel) {
      @Override
      public String toString() {
        return getTitle() + " " + super.toString();
      }
    };
  }

  @Nonnull
  public String getComponentName() {
    return "ProjectPane";
  }

  // should be first
  @Override
  public int getWeight() {
    return 0;
  }

  private final class ProjectViewTreeUpdater extends AbstractTreeUpdater {
    private ProjectViewTreeUpdater(final AbstractTreeBuilder treeBuilder) {
      super(treeBuilder);
    }

    @Override
    public boolean addSubtreeToUpdateByElement(Object element) {
      if (element instanceof PsiDirectory && !myProject.isDisposed()) {
        final PsiDirectory dir = (PsiDirectory)element;
        final ProjectTreeStructure treeStructure = (ProjectTreeStructure)myTreeStructure;
        PsiDirectory dirToUpdateFrom = dir;
        if (!treeStructure.isFlattenPackages() && treeStructure.isHideEmptyMiddlePackages()) {
          // optimization: this check makes sense only if flattenPackages == false && HideEmptyMiddle == true
          while (dirToUpdateFrom != null && BaseProjectViewDirectoryHelper.isEmptyMiddleDirectory(dirToUpdateFrom, true)) {
            dirToUpdateFrom = dirToUpdateFrom.getParentDirectory();
          }
        }
        boolean addedOk;
        while (!(addedOk = super.addSubtreeToUpdateByElement(dirToUpdateFrom == null? myTreeStructure.getRootElement() : dirToUpdateFrom))) {
          if (dirToUpdateFrom == null) {
            break;
          }
          dirToUpdateFrom = dirToUpdateFrom.getParentDirectory();
        }
        return addedOk;
      }

      return super.addSubtreeToUpdateByElement(element);
    }
  }

  private class ProjectViewPaneTreeStructure extends ProjectTreeStructure{
    public ProjectViewPaneTreeStructure() {
      super(ProjectViewPane.this.myProject, ID);
    }

    @Override
    protected AbstractTreeNode createRoot(final Project project, ViewSettings settings) {
      return new ProjectViewProjectNode(project, settings);
    }

    @Nonnull
    @Override
    public <T> T getViewOption(@Nonnull KeyWithDefaultValue<T> option) {
      T value = ProjectViewPane.this.getUserData(option);
      assert value != null;
      return value;
    }
  }
}
