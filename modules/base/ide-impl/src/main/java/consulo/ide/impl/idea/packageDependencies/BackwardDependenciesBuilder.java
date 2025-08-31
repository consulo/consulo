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
import consulo.application.ApplicationManager;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.project.ProjectUtil;
import consulo.application.util.function.Computable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiRecursiveElementVisitor;

import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author anna
 * @since 2005-01-16
 */
public class BackwardDependenciesBuilder extends DependenciesBuilder {
  private final AnalysisScope myForwardScope;

  public BackwardDependenciesBuilder(Project project, AnalysisScope scope) {
    this(project, scope, null);
  }

  public BackwardDependenciesBuilder(Project project, AnalysisScope scope, @Nullable AnalysisScope scopeOfInterest) {
    super(project, scope, scopeOfInterest);
    myForwardScope = ApplicationManager.getApplication().runReadAction(new Computable<AnalysisScope>() {
      @Override
      public AnalysisScope compute() {
        return getScope().getNarrowedComplementaryScope(getProject());
      }
    });
    myFileCount = myForwardScope.getFileCount();
    myTotalFileCount = myFileCount + scope.getFileCount();
  }

  @Override
  public String getRootNodeNameInUsageView() {
    return AnalysisScopeBundle.message("backward.dependencies.usage.view.root.node.text");
  }

  @Override
  public String getInitialUsagesPosition() {
    return AnalysisScopeBundle.message("backward.dependencies.usage.view.initial.text");
  }

  @Override
  public boolean isBackward() {
    return true;
  }

  @Override
  public void analyze() {
    AnalysisScope scope = myForwardScope;
    final DependenciesBuilder builder = new ForwardDependenciesBuilder(getProject(), scope, getScopeOfInterest());
    builder.setTotalFileCount(myTotalFileCount);
    builder.analyze();

    subtractScope(builder, getScope());
    final PsiManager psiManager = PsiManager.getInstance(getProject());
    psiManager.startBatchFilesProcessingMode();
    try {
      final int fileCount = getScope().getFileCount();
      getScope().accept(new PsiRecursiveElementVisitor() {
        @Override public void visitFile(PsiFile file) {
          ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
          if (indicator != null) {
            if (indicator.isCanceled()) {
              throw new ProcessCanceledException();
            }
            indicator.setText(AnalysisScopeBundle.message("package.dependencies.progress.text"));
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
              indicator.setText2(ProjectUtil.calcRelativeToProjectPath(virtualFile, getProject()));
            }
            if (fileCount > 0) {
              indicator.setFraction(((double)++myFileCount) / myTotalFileCount);
            }
          }
          Map<PsiFile, Set<PsiFile>> dependencies = builder.getDependencies();
          for (PsiFile psiFile : dependencies.keySet()) {
            if (dependencies.get(psiFile).contains(file)) {
              Set<PsiFile> fileDeps = getDependencies().get(file);
              if (fileDeps == null) {
                fileDeps = new HashSet<PsiFile>();
                getDependencies().put(file, fileDeps);
              }
              fileDeps.add(psiFile);
            }
          }
          psiManager.dropResolveCaches();
          InjectedLanguageManager.getInstance(file.getProject()).dropFileCaches(file);
        }
      });
    }
    finally {
      psiManager.finishBatchFilesProcessingMode();
    }
  }

  private static void subtractScope(DependenciesBuilder builders, AnalysisScope scope) {
    Map<PsiFile, Set<PsiFile>> dependencies = builders.getDependencies();

    Set<PsiFile> excluded = new HashSet<PsiFile>();

    for (PsiFile psiFile : dependencies.keySet()) {
      if (scope.contains(psiFile)) {
        excluded.add(psiFile);
      }
    }

    for ( PsiFile psiFile : excluded ) {
      dependencies.remove(psiFile);
    }
  }
}
