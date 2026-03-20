package consulo.execution.coverage.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class CoverageViewExtension {
    protected final Project myProject;
    private final CoverageSuitesBundle mySuitesBundle;
    private final CoverageViewManager.StateBean myStateBean;
    private final CoverageDataManager myCoverageDataManager;
    private final CoverageViewManager myCoverageViewManager;

    public CoverageViewExtension(Project project, CoverageSuitesBundle suitesBundle, CoverageViewManager.StateBean stateBean) {
        myProject = project;
        mySuitesBundle = suitesBundle;
        myStateBean = stateBean;
        myCoverageDataManager = CoverageDataManager.getInstance(myProject);
        myCoverageViewManager = CoverageViewManager.getInstance(myProject);
    }

    public Project getProject() {
        return myProject;
    }

    public CoverageSuitesBundle getSuitesBundle() {
        return mySuitesBundle;
    }

    public CoverageViewManager.StateBean getStateBean() {
        return myStateBean;
    }

    public CoverageDataManager getCoverageDataManager() {
        return myCoverageDataManager;
    }

    public CoverageViewManager getCoverageViewManager() {
        return myCoverageViewManager;
    }

    public abstract @Nullable String getSummaryForNode(AbstractTreeNode node);

    public abstract @Nullable String getSummaryForRootNode(AbstractTreeNode childNode);

    public abstract @Nullable String getPercentage(int columnIdx, AbstractTreeNode node);

    public abstract List<AbstractTreeNode> getChildrenNodes(AbstractTreeNode node);

    public abstract ColumnInfo[] createColumnInfos();

    public abstract @Nullable PsiElement getParentElement(PsiElement element);

    public abstract AbstractTreeNode createRootNode();

    @RequiredReadAction
    public boolean canSelectInCoverageView(Object object) {
        return object instanceof VirtualFile virtualFile && PsiManager.getInstance(myProject).findFile(virtualFile) != null;
    }

    @RequiredReadAction
    public @Nullable PsiElement getElementToSelect(Object object) {
        if (object instanceof PsiElement element) {
            return element;
        }
        return object instanceof VirtualFile virtualFile ? PsiManager.getInstance(myProject).findFile(virtualFile) : null;
    }

    public @Nullable VirtualFile getVirtualFile(Object object) {
        return switch (object) {
            case PsiDirectory directory -> directory.getVirtualFile();
            case PsiElement element -> {
                PsiFile containingFile = element.getContainingFile();
                yield containingFile != null ? containingFile.getVirtualFile() : null;
            }
            case VirtualFile virtualFile -> virtualFile;
            default -> null;
        };
    }

    public List<AbstractTreeNode> createTopLevelNodes() {
        return Collections.emptyList();
    }

    public boolean supportFlattenPackages() {
        return false;
    }
}