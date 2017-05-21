package com.intellij.coverage.view;

import com.intellij.coverage.CoverageAnnotator;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ui.ColumnInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: anna
 * Date: 1/9/12
 */
public class DirectoryCoverageViewExtension extends CoverageViewExtension {
  private final CoverageAnnotator myAnnotator;

  public DirectoryCoverageViewExtension(Project project,
                                        CoverageAnnotator annotator,
                                        CoverageSuitesBundle suitesBundle,
                                        CoverageViewManager.StateBean stateBean) {
    super(project, suitesBundle, stateBean);
    myAnnotator = annotator;
  }

  @Override
  public ColumnInfo[] createColumnInfos() {
    return new ColumnInfo[]{new ElementColumnInfo(), new PercentageCoverageColumnInfo(1, "Statistics, %", getSuitesBundle(), getStateBean())};
  }

  @Override
  public String getSummaryForNode(AbstractTreeNode node) {
    return "Coverage Summary for \'" + node.toString() + "\': " +
           myAnnotator.getDirCoverageInformationString((PsiDirectory)node.getValue(), getSuitesBundle(),
                                                       getCoverageDataManager());
  }

  @Override
  public String getSummaryForRootNode(AbstractTreeNode childNode) {
    final Object value = childNode.getValue();
    String coverageInformationString = myAnnotator.getDirCoverageInformationString(((PsiDirectory)value), getSuitesBundle(),
                                                                                   getCoverageDataManager());

    return "Coverage Summary: " + coverageInformationString;
  }

  @Override
  public String getPercentage(int columnIdx, AbstractTreeNode node) {
    final Object value = node.getValue();
    if (value instanceof PsiFile) {
      return myAnnotator.getFileCoverageInformationString((PsiFile)value, getSuitesBundle(), getCoverageDataManager());
    }
    return value != null ? myAnnotator.getDirCoverageInformationString((PsiDirectory)value, getSuitesBundle(), getCoverageDataManager()) : null;
  }


  @Override
  public PsiElement getParentElement(PsiElement element) {
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile != null) {
      return containingFile.getContainingDirectory();
    }
    return null;
  }

  @Override
  public AbstractTreeNode createRootNode() {
    final VirtualFile baseDir = getProject().getBaseDir();
    return new CoverageListRootNode(getProject(), PsiManager.getInstance(getProject()).findDirectory(baseDir), getSuitesBundle(), getStateBean());
  }

  @Override
  public List<AbstractTreeNode> getChildrenNodes(AbstractTreeNode node) {
    List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
    if (node instanceof CoverageListNode) {
      final Object val = node.getValue();
      if (val instanceof PsiFile) return Collections.emptyList();
      final PsiDirectory psiDirectory = (PsiDirectory)val;
      final PsiDirectory[] subdirectories = ApplicationManager.getApplication().runReadAction(new Computable<PsiDirectory[]>() {
        @Override
        public PsiDirectory[] compute() {
          return psiDirectory.getSubdirectories(); 
        }
      });
      for (PsiDirectory subdirectory : subdirectories) {
        children.add(new CoverageListNode(getProject(), subdirectory, getSuitesBundle(), getStateBean()));
      }
      final PsiFile[] psiFiles = ApplicationManager.getApplication().runReadAction(new Computable<PsiFile[]>() {
        @Override
        public PsiFile[] compute() {
          return psiDirectory.getFiles();
        }
      });
      for (PsiFile psiFile : psiFiles) {
        children.add(new CoverageListNode(getProject(), psiFile, getSuitesBundle(), getStateBean()));
      }

      for (AbstractTreeNode childNode : children) {
        childNode.setParent(node);
      }
    }
    return children;
  }
}
