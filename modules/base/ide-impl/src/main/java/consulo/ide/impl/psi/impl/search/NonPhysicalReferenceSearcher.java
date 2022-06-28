// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl.search;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.scratch.ScratchFileService;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.language.psi.search.ReferencesSearch;
import consulo.project.Project;
import consulo.project.util.query.QueryExecutorBase;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;

/**
 * @author Gregory.Shrago
 * This searcher does the job for various console and fragment editors and other non-physical files.
 * We need this because ScopeEnlarger functionality will not work for nonphysical files.
 */
@ExtensionImpl
public class NonPhysicalReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> implements ReferencesSearchQueryExecutor {

  public NonPhysicalReferenceSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@Nonnull ReferencesSearch.SearchParameters queryParameters, @Nonnull Processor<? super PsiReference> consumer) {
    final SearchScope scope = queryParameters.getScopeDeterminedByUser();
    final PsiElement element = queryParameters.getElementToSearch();
    final PsiFile containingFile = element.getContainingFile();
    if (!(scope instanceof GlobalSearchScope) && !isApplicableTo(containingFile)) {
      return;
    }
    final LocalSearchScope currentScope;
    if (scope instanceof LocalSearchScope) {
      if (queryParameters.isIgnoreAccessScope()) {
        return;
      }
      currentScope = (LocalSearchScope)scope;
    }
    else {
      currentScope = null;
    }
    Project project = element.getProject();
    if (!project.isInitialized()) {
      return; // skip default and other projects that look weird
    }
    final PsiManager psiManager = PsiManager.getInstance(project);
    for (VirtualFile virtualFile : FileEditorManager.getInstance(project).getOpenFiles()) {
      if (!virtualFile.isValid()) continue;
      if (virtualFile.getFileType().isBinary()) continue;
      PsiFile file = psiManager.findFile(virtualFile);
      if (isApplicableTo(file)) {
        final LocalSearchScope fileScope = new LocalSearchScope(file);
        final LocalSearchScope searchScope = currentScope == null ? fileScope : fileScope.intersectWith(currentScope);
        ReferencesSearch.searchOptimized(element, searchScope, true, queryParameters.getOptimizer(), consumer);
      }
    }
  }

  private static boolean isApplicableTo(PsiFile file) {
    if (file == null) {
      return false;
    }
    return (!file.getViewProvider().isPhysical() && !(file instanceof PsiCodeFragment)) || ScratchFileService.getInstance().getRootType(file.getVirtualFile()) != null;
  }
}
