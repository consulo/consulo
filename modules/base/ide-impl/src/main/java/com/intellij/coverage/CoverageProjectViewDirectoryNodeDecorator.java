package com.intellij.coverage;

import consulo.ui.ex.tree.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPsiElementPointer;
import com.intellij.ui.ColoredTreeCellRenderer;
import jakarta.inject.Inject;

/**
 * @author yole
 */
public class CoverageProjectViewDirectoryNodeDecorator extends AbstractCoverageProjectViewNodeDecorator {
  @Inject
  public CoverageProjectViewDirectoryNodeDecorator(final CoverageDataManager coverageDataManager) {
    super(coverageDataManager);
  }

  @Override
  public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {
    final PsiElement element = node.getPsiElement();
    if (element == null || !element.isValid()) {
      return;
    }

    final CoverageDataManager manager = getCoverageDataManager();
    final CoverageSuitesBundle currentSuite = manager.getCurrentSuitesBundle();
    final CoverageAnnotator coverageAnnotator = currentSuite != null ? currentSuite.getAnnotator(element.getProject()) : null;
    if (coverageAnnotator == null) {
      // N/A
      return;
    }

    if (element instanceof PsiDirectory) {
      final String informationString = coverageAnnotator.getDirCoverageInformationString((PsiDirectory) element, currentSuite, manager);
      if (informationString != null) {
        appendCoverageInfo(cellRenderer, informationString);
      }
    }
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
      element = (PsiElement)value;
    }
    else if (value instanceof SmartPsiElementPointer) {
      element = ((SmartPsiElementPointer)value).getElement();
    }

    String informationString = null;
    if (element instanceof PsiDirectory) {
      informationString = coverageAnnotator.getDirCoverageInformationString((PsiDirectory)element, currentSuite, manager);
    } else if (element instanceof PsiFile) {
      informationString = coverageAnnotator.getFileCoverageInformationString((PsiFile)element, currentSuite, manager);
    }

    if (informationString != null) {
      data.setLocationString(informationString);
    }
  }

}
