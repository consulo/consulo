// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.ide.impl.ProjectPaneSelectInTarget;
import consulo.ide.impl.idea.ide.projectView.BaseProjectTreeBuilder;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.ProjectViewProjectNode;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.language.psi.PsiDirectory;
import consulo.localize.LocalizeValue;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.BaseProjectViewDirectoryHelper;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.awt.tree.AbstractTreeUpdater;
import consulo.util.dataholder.KeyWithDefaultValue;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.tree.DefaultTreeModel;

@ExtensionImpl
public class ProjectViewPaneImpl extends AbstractProjectViewPSIPane {
    public static final String ID = "ProjectPane";

    @Inject
    public ProjectViewPaneImpl(Project project) {
        super(project);
    }

    @Nonnull
    @Override
    public LocalizeValue getTitle() {
        return IdeLocalize.titleProject();
    }

    @Override
    @Nonnull
    public String getId() {
        return ID;
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
                    while (dirToUpdateFrom != null
                        && BaseProjectViewDirectoryHelper.isEmptyMiddleDirectory(dirToUpdateFrom, true)) {
                        dirToUpdateFrom = dirToUpdateFrom.getParentDirectory();
                    }
                }
                boolean addedOk;
                while (!(addedOk =
                    super.addSubtreeToUpdateByElement(dirToUpdateFrom == null ? myTreeStructure.getRootElement() : dirToUpdateFrom))) {
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
            super(ProjectViewPaneImpl.this.myProject, ID);
        }

        @Override
        protected AbstractTreeNode createRoot(@Nonnull final Project project, @Nonnull ViewSettings settings) {
            return new ProjectViewProjectNode(project, settings);
        }

        @Nonnull
        @Override
        public <T> T getViewOption(@Nonnull KeyWithDefaultValue<T> option) {
            T value = ProjectViewPaneImpl.this.getUserData(option);
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
        return (archiveFile != null && index.getContentRootForFile(archiveFile, false) != null)
            || index.getContentRootForFile(file, false) != null
            || index.isInLibrary(file)
            || Comparing.equal(file.getParent(), project.getBaseDir())
            || ScratchUtil.isScratch(file);
    }
}
