package consulo.execution.coverage.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.coverage.CoverageAnnotator;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.view.AbstractCoverageProjectViewNodeDecorator;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.ui.ex.tree.PresentationData;
import jakarta.inject.Inject;

/**
 * @author yole
 */
@ExtensionImpl
public class CoverageProjectViewDirectoryNodeDecorator extends AbstractCoverageProjectViewNodeDecorator {
    @Inject
    public CoverageProjectViewDirectoryNodeDecorator(CoverageDataManager coverageDataManager) {
        super(coverageDataManager);
    }

    @Override
    @RequiredReadAction
    public void decorate(ProjectViewNode node, PresentationData data) {
        CoverageDataManager manager = getCoverageDataManager();
        CoverageSuitesBundle currentSuite = manager.getCurrentSuitesBundle();
        CoverageAnnotator coverageAnnotator = currentSuite != null ? currentSuite.getAnnotator(node.getProject()) : null;
        if (coverageAnnotator == null) {
            // N/A
            return;
        }

        Object value = node.getValue();
        PsiElement element = null;
        if (value instanceof PsiElement psiElement) {
            element = psiElement;
        }
        else if (value instanceof SmartPsiElementPointer elementPointer) {
            element = elementPointer.getElement();
        }

        String informationString = null;
        if (element instanceof PsiDirectory directory) {
            informationString = coverageAnnotator.getDirCoverageInformationString(directory, currentSuite, manager);
        }
        else if (element instanceof PsiFile file) {
            informationString = coverageAnnotator.getFileCoverageInformationString(file, currentSuite, manager);
        }

        if (informationString != null) {
            data.setLocationString(informationString);
        }
    }
}
