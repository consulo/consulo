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

import jakarta.annotation.Nullable;

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

    @Nullable
    public abstract String getSummaryForNode(AbstractTreeNode node);

    @Nullable
    public abstract String getSummaryForRootNode(AbstractTreeNode childNode);

    @Nullable
    public abstract String getPercentage(int columnIdx, AbstractTreeNode node);

    public abstract List<AbstractTreeNode> getChildrenNodes(AbstractTreeNode node);

    public abstract ColumnInfo[] createColumnInfos();

    @Nullable
    public abstract PsiElement getParentElement(PsiElement element);

    public abstract AbstractTreeNode createRootNode();

    @RequiredReadAction
    public boolean canSelectInCoverageView(Object object) {
        return object instanceof VirtualFile virtualFile && PsiManager.getInstance(myProject).findFile(virtualFile) != null;
    }

    @Nullable
    @RequiredReadAction
    public PsiElement getElementToSelect(Object object) {
        if (object instanceof PsiElement element) {
            return element;
        }
        return object instanceof VirtualFile virtualFile ? PsiManager.getInstance(myProject).findFile(virtualFile) : null;
    }

    @Nullable
    public VirtualFile getVirtualFile(Object object) {
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