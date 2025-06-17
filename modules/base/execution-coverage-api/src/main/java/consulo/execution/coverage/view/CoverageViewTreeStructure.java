package consulo.execution.coverage.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

/**
 * @author anna
 * @since 2012-01-02
 */
public class CoverageViewTreeStructure extends AbstractTreeStructure {
    private final Project myProject;
    final CoverageSuitesBundle myData;
    final CoverageViewManager.StateBean myStateBean;
    private final CoverageListRootNode myRootNode;
    private final CoverageViewExtension myCoverageViewExtension;

    public CoverageViewTreeStructure(Project project, CoverageSuitesBundle bundle, CoverageViewManager.StateBean stateBean) {
        myProject = project;
        myData = bundle;
        myStateBean = stateBean;
        myCoverageViewExtension = myData.getCoverageEngine().createCoverageViewExtension(project, bundle, stateBean);
        myRootNode = (CoverageListRootNode) myCoverageViewExtension.createRootNode();
    }

    @Nonnull
    @Override
    public Object getRootElement() {
        return myRootNode;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Object[] getChildElements(@Nonnull Object element) {
        return getChildren(element, myData, myStateBean);
    }

    @RequiredReadAction
    static Object[] getChildren(
        Object element,
        CoverageSuitesBundle bundle,
        CoverageViewManager.StateBean stateBean
    ) {
        if (element instanceof CoverageListRootNode coverageListRootNode && stateBean.myFlattenPackages) {
            Collection<? extends AbstractTreeNode> children = coverageListRootNode.getChildren();
            return children.toArray(new Object[children.size()]);
        }
        if (element instanceof CoverageListNode coverageListNode) {
            List<AbstractTreeNode> children =
                bundle.getCoverageEngine().createCoverageViewExtension(coverageListNode.getProject(), bundle, stateBean)
                    .getChildrenNodes(coverageListNode);
            return children.toArray(new CoverageListNode[children.size()]);
        }
        return null;
    }

    @Override
    public Object getParentElement(@Nonnull Object element) {
        PsiElement psiElement = (PsiElement) element;
        return myCoverageViewExtension.getParentElement(psiElement);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public CoverageViewDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
        return new CoverageViewDescriptor(myProject, parentDescriptor, element);
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    public boolean supportFlattenPackages() {
        return myCoverageViewExtension.supportFlattenPackages();
    }
}

