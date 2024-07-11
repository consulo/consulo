// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.ui.internal.scope;

import consulo.content.scope.SearchScope;
import consulo.find.FindSettings;
import consulo.language.editor.internal.ModelScopeItem;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.ui.scope.AnalysisUIOptions;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

public final class CustomScopeItem implements ModelScopeItem {
  private final Project myProject;
  private boolean mySearchInLib;
  private String myPreselect;
  private Supplier<? extends SearchScope> mySupplierScope;

  public CustomScopeItem(Project project, @Nullable PsiElement context) {
    myProject = project;

    AnalysisUIOptions options = AnalysisUIOptions.getInstance(project);
    VirtualFile file = PsiUtilCore.getVirtualFile(context);
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    mySearchInLib = file != null && fileIndex.isInLibrary(file);

    myPreselect = StringUtil.isEmptyOrSpaces(options.CUSTOM_SCOPE_NAME)
      ? FindSettings.getInstance().getDefaultScopeName()
      : options.CUSTOM_SCOPE_NAME;
    if (mySearchInLib && GlobalSearchScope.projectScope(myProject).getDisplayName().equals(myPreselect)) {
      myPreselect = GlobalSearchScope.allScope(myProject).getDisplayName();
    }
    if (GlobalSearchScope.allScope(myProject).getDisplayName().equals(myPreselect) && options.SCOPE_TYPE == AnalysisScope.CUSTOM) {
      options.CUSTOM_SCOPE_NAME = myPreselect;
      mySearchInLib = true;
    }
  }

  public Project getProject() {
    return myProject;
  }

  public boolean getSearchInLibFlag() {
    return mySearchInLib;
  }

  public String getPreselectedCustomScope() {
    return myPreselect;
  }

  @Override
  public AnalysisScope getScope() {
    if (mySupplierScope != null)
      return new AnalysisScope(mySupplierScope.get(), myProject);
    return null;
  }

  public void setSearchScopeSupplier(Supplier<? extends SearchScope> supplier) {
    mySupplierScope = supplier;
  }
}
