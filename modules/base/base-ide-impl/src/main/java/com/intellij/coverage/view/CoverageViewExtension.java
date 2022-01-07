package com.intellij.coverage.view;

import com.intellij.coverage.CoverageDataManager;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ui.ColumnInfo;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class CoverageViewExtension {

  private final Project myProject;
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

  @javax.annotation.Nullable
  public abstract String getSummaryForNode(AbstractTreeNode node);

  @javax.annotation.Nullable
  public abstract String getSummaryForRootNode(AbstractTreeNode childNode);

  @Nullable
  public abstract String getPercentage(int columnIdx, AbstractTreeNode node);

  public abstract List<AbstractTreeNode> getChildrenNodes(AbstractTreeNode node);

  public abstract ColumnInfo[] createColumnInfos();

  @javax.annotation.Nullable
  public abstract PsiElement getParentElement(PsiElement element);

  public abstract AbstractTreeNode createRootNode();
  
  public boolean canSelectInCoverageView(Object object) {
    return object instanceof VirtualFile && PsiManager.getInstance(myProject).findFile((VirtualFile)object) != null;
  }
  
  @javax.annotation.Nullable
  public PsiElement getElementToSelect(Object object) {
    if (object instanceof PsiElement) return (PsiElement)object;
    return object instanceof VirtualFile ? PsiManager.getInstance(myProject).findFile((VirtualFile)object) : null;
  }

  @javax.annotation.Nullable
  public VirtualFile getVirtualFile(Object object) {
    if (object instanceof PsiElement) {
      if (object instanceof PsiDirectory) return ((PsiDirectory)object).getVirtualFile();
      final PsiFile containingFile = ((PsiElement)object).getContainingFile();
      if (containingFile != null) {
        return containingFile.getVirtualFile();
      }
      return null;
    }
    return object instanceof VirtualFile ? (VirtualFile)object : null;
  }

  public List<AbstractTreeNode> createTopLevelNodes() {
    return Collections.emptyList();
  }

  public boolean supportFlattenPackages() {
    return false;
  }
}