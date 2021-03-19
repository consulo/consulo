// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.impl.ProjectPaneSelectInTarget;
import com.intellij.ide.projectView.BaseProjectTreeBuilder;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.BaseProjectViewDirectoryHelper;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import consulo.ui.image.Image;
import consulo.util.dataholder.KeyWithDefaultValue;
import consulo.util.lang.Comparing;
import consulo.vfs.ArchiveFileSystem;
import consulo.vfs.util.ArchiveVfsUtil;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.swing.plaf.FontUIResource;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

public class ProjectViewPane extends AbstractProjectViewPSIPane {
  public static final String ID = "ProjectPane";

  @Inject
  public ProjectViewPane(Project project) {
    super(project);
  }

  @Nonnull
  @Override
  public String getTitle() {
    return IdeBundle.message("title.project");
  }

  @Override
  @Nonnull
  public String getId() {
    return ID;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.General.ProjectTab;
  }


  @Nonnull
  @Override
  public SelectInTarget createSelectInTarget() {
    return new ProjectPaneSelectInTarget(myProject);
  }

  @Nonnull
  @Override
  protected AbstractTreeUpdater createTreeUpdater(@Nonnull AbstractTreeBuilder treeBuilder) {
    return new ProjectViewTreeUpdater(treeBuilder);
  }

  @Nonnull
  @Override
  public ProjectAbstractTreeStructureBase createStructure() {
    return new ProjectViewPaneTreeStructure();
  }

  @Nonnull
  @Override
  protected ProjectViewTree createTree(@Nonnull DefaultTreeModel treeModel) {
    return new ProjectViewTree(treeModel) {
      @Override
      public void updateUI() {
        super.updateUI();

        if (Registry.is("bigger.font.in.project.view")) {
          Font font = getFont();
          setFont(new FontUIResource(font.deriveFont(font.getSize() + 1f)));
        }
      }

      @Override
      public String toString() {
        return getTitle() + " " + super.toString();
      }
    };
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
    public boolean addSubtreeToUpdateByElement(@Nonnull Object element) {
      if (element instanceof PsiDirectory && !myProject.isDisposed()) {
        final PsiDirectory dir = (PsiDirectory)element;
        final ProjectTreeStructure treeStructure = (ProjectTreeStructure)myTreeStructure;
        PsiDirectory dirToUpdateFrom = dir;

        // optimization
        // isEmptyMiddleDirectory can be slow when project VFS is not fully loaded (initial dumb mode).
        // It's easiest to disable the optimization in any dumb mode
        if (!treeStructure.isFlattenPackages() && treeStructure.isHideEmptyMiddlePackages() && !DumbService.isDumb(myProject)) {
          while (dirToUpdateFrom != null && BaseProjectViewDirectoryHelper.isEmptyMiddleDirectory(dirToUpdateFrom, true)) {
            dirToUpdateFrom = dirToUpdateFrom.getParentDirectory();
          }
        }
        boolean addedOk;
        while (!(addedOk = super.addSubtreeToUpdateByElement(dirToUpdateFrom == null ? myTreeStructure.getRootElement() : dirToUpdateFrom))) {
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

  private class ProjectViewPaneTreeStructure extends ProjectTreeStructure {
    public ProjectViewPaneTreeStructure() {
      super(ProjectViewPane.this.myProject, ID);
    }

    @Override
    protected AbstractTreeNode createRoot(@Nonnull final Project project, @Nonnull ViewSettings settings) {
      return new ProjectViewProjectNode(project, settings);
    }

    @Nonnull
    @Override
    public <T> T getViewOption(@Nonnull KeyWithDefaultValue<T> option) {
      T value = ProjectViewPane.this.getUserData(option);
      assert value != null;
      return value;
    }


    @Override
    public boolean isToBuildChildrenInBackground(@Nonnull Object element) {
      return Registry.is("ide.projectView.ProjectViewPaneTreeStructure.BuildChildrenInBackground");
    }
  }

  @Override
  protected BaseProjectTreeBuilder createBuilder(@Nonnull DefaultTreeModel model) {
    return null;
  }

  public static boolean canBeSelectedInProjectView(@Nonnull Project project, @Nonnull VirtualFile file) {
    final VirtualFile archiveFile;

    if (file.getFileSystem() instanceof ArchiveFileSystem) {
      archiveFile = ArchiveVfsUtil.getVirtualFileForArchive(file);
    }
    else {
      archiveFile = null;
    }

    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    return (archiveFile != null && index.getContentRootForFile(archiveFile, false) != null) ||
           index.getContentRootForFile(file, false) != null ||
           index.isInLibrary(file) ||
           Comparing.equal(file.getParent(), project.getBaseDir()) ||
           ScratchUtil.isScratch(file);
  }

}
