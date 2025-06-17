package consulo.execution.coverage.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.execution.coverage.CoverageAnnotator;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author anna
 * @since 2012-01-09
 */
public class DirectoryCoverageViewExtension extends CoverageViewExtension {
    private final CoverageAnnotator myAnnotator;

    public DirectoryCoverageViewExtension(
        Project project,
        CoverageAnnotator annotator,
        CoverageSuitesBundle suitesBundle,
        CoverageViewManager.StateBean stateBean
    ) {
        super(project, suitesBundle, stateBean);
        myAnnotator = annotator;
    }

    @Override
    public ColumnInfo[] createColumnInfos() {
        return new ColumnInfo[]{new ElementColumnInfo(), new PercentageCoverageColumnInfo(
            1,
            "Statistics, %",
            getSuitesBundle(),
            getStateBean()
        )};
    }

    @Override
    public String getSummaryForNode(AbstractTreeNode node) {
        return "Coverage Summary for \'" + node.toString() + "\': " +
            myAnnotator.getDirCoverageInformationString((PsiDirectory) node.getValue(), getSuitesBundle(),
                getCoverageDataManager()
            );
    }

    @Override
    public String getSummaryForRootNode(AbstractTreeNode childNode) {
        Object value = childNode.getValue();
        String coverageInformationString =
            myAnnotator.getDirCoverageInformationString(((PsiDirectory) value), getSuitesBundle(), getCoverageDataManager());

        return "Coverage Summary: " + coverageInformationString;
    }

    @Override
    public String getPercentage(int columnIdx, AbstractTreeNode node) {
        Object value = node.getValue();
        if (value instanceof PsiFile file) {
            return myAnnotator.getFileCoverageInformationString(file, getSuitesBundle(), getCoverageDataManager());
        }
        return value != null
            ? myAnnotator.getDirCoverageInformationString((PsiDirectory) value, getSuitesBundle(), getCoverageDataManager())
            : null;
    }


    @Override
    public PsiElement getParentElement(PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile != null) {
            return containingFile.getContainingDirectory();
        }
        return null;
    }

    @Override
    @RequiredReadAction
    public AbstractTreeNode createRootNode() {
        VirtualFile baseDir = getProject().getBaseDir();
        return new CoverageListRootNode(
            getProject(),
            PsiManager.getInstance(getProject()).findDirectory(baseDir),
            getSuitesBundle(),
            getStateBean()
        );
    }

    @Override
    public List<AbstractTreeNode> getChildrenNodes(AbstractTreeNode node) {
        List<AbstractTreeNode> children = new ArrayList<>();
        if (node instanceof CoverageListNode) {
            Object val = node.getValue();
            if (val instanceof PsiFile) {
                return Collections.emptyList();
            }
            PsiDirectory psiDirectory = (PsiDirectory) val;
            PsiDirectory[] subdirectories =
                myProject.getApplication().runReadAction((Supplier<PsiDirectory[]>) () -> psiDirectory.getSubdirectories());
            for (PsiDirectory subdirectory : subdirectories) {
                children.add(new CoverageListNode(getProject(), subdirectory, getSuitesBundle(), getStateBean()));
            }
            PsiFile[] files =
                myProject.getApplication().runReadAction((Supplier<PsiFile[]>) () -> psiDirectory.getFiles());
            for (PsiFile file : files) {
                children.add(new CoverageListNode(getProject(), file, getSuitesBundle(), getStateBean()));
            }

            for (AbstractTreeNode childNode : children) {
                childNode.setParent(node);
            }
        }
        return children;
    }
}
