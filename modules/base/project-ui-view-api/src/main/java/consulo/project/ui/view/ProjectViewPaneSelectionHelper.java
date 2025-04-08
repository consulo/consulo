// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.ui.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.TreePath;
import java.util.List;

/**
 * Implement this extension to customise selection process in the project view.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ProjectViewPaneSelectionHelper {
    public static final ExtensionPointName<ProjectViewPaneSelectionHelper> EP_NAME =
        ExtensionPointName.create(ProjectViewPaneSelectionHelper.class);

    /**
     * @param selectionDescriptor information about target elements and potential {@link TreePath tree paths} for selection found by {@link AbstractProjectViewPane#createVisitor(PsiElement, VirtualFile, List)}  node visitor}
     * @return collection of tree paths to select in tree or null if this helper can't handle {@link SelectionDescriptor}
     * @see AbstractTreeNode#canRepresent
     */
    @Nullable
    public abstract List<? extends TreePath> computeAdjustedPaths(@Nonnull SelectionDescriptor selectionDescriptor);

    /**
     * @param selectionDescriptor information about target elements and potential {@link TreePath tree paths} for selection found by {@link AbstractProjectViewPane#createVisitor(PsiElement, VirtualFile, List)}  node visitor}
     * @return list of {@link TreePath tree paths} to select, computed from {@code selectionDescriptor} with first suitable selection helper
     * Returns {@link SelectionDescriptor#originalTreePaths original paths} by default
     */
    @Nonnull
    public static List<? extends TreePath> getAdjustedPaths(@Nonnull SelectionDescriptor selectionDescriptor) {
        for (ProjectViewPaneSelectionHelper helper : EP_NAME.getExtensionList()) {
            List<? extends TreePath> adjustedPaths = helper.computeAdjustedPaths(selectionDescriptor);
            if (adjustedPaths != null) {
                return adjustedPaths;
            }
        }
        return selectionDescriptor.originalTreePaths;
    }

    public static class SelectionDescriptor {
        @Nullable
        public final PsiElement targetPsiElement;
        @Nullable
        public final VirtualFile targetVirtualFile;
        @Nonnull
        public final List<TreePath> originalTreePaths;

        public SelectionDescriptor(
            @Nullable PsiElement targetPsiElement,
            @Nullable VirtualFile targetVirtualFile,
            @Nonnull List<TreePath> originalTreePaths
        ) {
            this.targetPsiElement = targetPsiElement;
            this.targetVirtualFile = targetVirtualFile;
            this.originalTreePaths = List.copyOf(originalTreePaths);
        }
    }
}
