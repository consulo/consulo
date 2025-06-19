package consulo.ide.impl.idea.coverage;

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
    public CoverageProjectViewDirectoryNodeDecorator(final CoverageDataManager coverageDataManager) {
        super(coverageDataManager);
    }

    public void decorate(ProjectViewNode node, PresentationData data) {
        final CoverageDataManager manager = getCoverageDataManager();
        final CoverageSuitesBundle currentSuite = manager.getCurrentSuitesBundle();
        final CoverageAnnotator coverageAnnotator = currentSuite != null ? currentSuite.getAnnotator(node.getProject())
            : null;
        if (coverageAnnotator == null) {
            // N/A
            return;
        }

        final Object value = node.getValue();
        PsiElement element = null;
        if (value instanceof PsiElement) {
            element = (PsiElement) value;
        }
        else if (value instanceof SmartPsiElementPointer) {
            element = ((SmartPsiElementPointer) value).getElement();
        }

        String informationString = null;
        if (element instanceof PsiDirectory) {
            informationString = coverageAnnotator.getDirCoverageInformationString((PsiDirectory) element, currentSuite, manager);
        }
        else if (element instanceof PsiFile) {
            informationString = coverageAnnotator.getFileCoverageInformationString((PsiFile) element, currentSuite, manager);
        }

        if (informationString != null) {
            data.setLocationString(informationString);
        }
    }
}
