/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.ide.impl.idea.packageDependencies;

import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.AnalysisScopeBundle;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.project.ProjectUtil;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ForwardDependenciesBuilder extends DependenciesBuilder {
  private final Map<PsiFile, Set<PsiFile>> myDirectDependencies = new HashMap<PsiFile, Set<PsiFile>>();

  public ForwardDependenciesBuilder(@Nonnull Project project, @Nonnull AnalysisScope scope) {
    super(project, scope);
  }

  public ForwardDependenciesBuilder(final Project project, final AnalysisScope scope, final AnalysisScope scopeOfInterest) {
    super(project, scope, scopeOfInterest);
  }

  public ForwardDependenciesBuilder(final Project project, final AnalysisScope scope, final int transitive) {
    super(project, scope);
    myTransitive = transitive;
  }

  @Override
  public String getRootNodeNameInUsageView(){
    return AnalysisScopeBundle.message("forward.dependencies.usage.view.root.node.text");
  }

  @Override
  public String getInitialUsagesPosition(){
    return AnalysisScopeBundle.message("forward.dependencies.usage.view.initial.text");
  }

  @Override
  public boolean isBackward(){
    return false;
  }

  @Override
  public void analyze() {
    final PsiManager psiManager = PsiManager.getInstance(getProject());
    psiManager.startBatchFilesProcessingMode();
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
    try {
      getScope().accept(new PsiRecursiveElementVisitor() {
        @Override public void visitFile(final PsiFile file) {
          visit(file, fileIndex, psiManager, 0);
        }
      });
    }
    finally {
      psiManager.finishBatchFilesProcessingMode();
    }
  }

  private void visit(final PsiFile file, final ProjectFileIndex fileIndex, final PsiManager psiManager, int depth) {

    final FileViewProvider viewProvider = file.getViewProvider();
    if (viewProvider.getBaseLanguage() != file.getLanguage()) return;

    if (getScopeOfInterest() != null && !getScopeOfInterest().contains(file)) return;

    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    final VirtualFile virtualFile = file.getVirtualFile();
    if (indicator != null) {
      if (indicator.isCanceled()) {
        throw new ProcessCanceledException();
      }
      indicator.setText(AnalysisScopeBundle.message("package.dependencies.progress.text"));

      if (virtualFile != null) {
        indicator.setText2(ProjectUtil.calcRelativeToProjectPath(virtualFile, getProject()));
      }
      if ( myTotalFileCount > 0) {
        indicator.setFraction(((double)++ myFileCount) / myTotalFileCount);
      }
    }

    final boolean isInLibrary =  virtualFile == null || fileIndex.isInLibrarySource(virtualFile) || fileIndex.isInLibraryClasses(virtualFile);
    final Set<PsiFile> collectedDeps = new HashSet<PsiFile>();
    final HashSet<PsiFile> processed = new HashSet<PsiFile>();
    collectedDeps.add(file);
    do {
      if (depth++ > getTransitiveBorder()) return;
      for (PsiFile psiFile : new HashSet<PsiFile>(collectedDeps)) {
        final VirtualFile vFile = psiFile.getVirtualFile();
        if (vFile != null) {

          if (indicator != null) {
            indicator.setText2(ProjectUtil.calcRelativeToProjectPath(vFile, getProject()));
          }

          if (!isInLibrary && (fileIndex.isInLibraryClasses(vFile) || fileIndex.isInLibrarySource(vFile))) {
            processed.add(psiFile);
          }
        }
        final Set<PsiFile> found = new HashSet<PsiFile>();
        if (!processed.contains(psiFile)) {
          processed.add(psiFile);
          analyzeFileDependencies(psiFile, new DependencyProcessor() {
            @Override
            public void process(PsiElement place, PsiElement dependency) {
              PsiFile dependencyFile = dependency.getContainingFile();
              if (dependencyFile != null) {
                if (viewProvider == dependencyFile.getViewProvider()) return;
                if (dependencyFile.isPhysical()) {
                  final VirtualFile virtualFile = dependencyFile.getVirtualFile();
                  if (virtualFile != null &&
                      (fileIndex.isInContent(virtualFile) ||
                       fileIndex.isInLibraryClasses(virtualFile) ||
                       fileIndex.isInLibrarySource(virtualFile))) {
                    found.add(dependencyFile);
                  }
                }
              }
            }
          });
          Set<PsiFile> deps = getDependencies().get(file);
          if (deps == null) {
            deps = new HashSet<PsiFile>();
            getDependencies().put(file, deps);
          }
          deps.addAll(found);

          getDirectDependencies().put(psiFile, new HashSet<PsiFile>(found));

          collectedDeps.addAll(found);

          psiManager.dropResolveCaches();
          InjectedLanguageManager.getInstance(file.getProject()).dropFileCaches(file);
        }
      }
      collectedDeps.removeAll(processed);
    }
    while (isTransitive() && !collectedDeps.isEmpty());
  }

  @Override
  public Map<PsiFile, Set<PsiFile>> getDirectDependencies() {
    return myDirectDependencies;
  }

}
