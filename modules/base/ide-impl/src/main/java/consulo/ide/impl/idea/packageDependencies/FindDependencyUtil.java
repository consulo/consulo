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

import consulo.language.editor.scope.AnalysisScopeBundle;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.usage.UsageInfo;

import jakarta.annotation.Nullable;
import java.util.*;

public class FindDependencyUtil {
  private FindDependencyUtil() {}

  public static UsageInfo[] findDependencies(@Nullable List<DependenciesBuilder> builders, Set<PsiFile> searchIn, Set<PsiFile> searchFor) {
    final List<UsageInfo> usages = new ArrayList<UsageInfo>();
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    int totalCount = searchIn.size();
    int count = 0;

    nextFile: for (PsiFile psiFile : searchIn) {
      count = updateIndicator(indicator, totalCount, count, psiFile);

      if (!psiFile.isValid()) continue;

      final Set<PsiFile> precomputedDeps;
      if (builders != null) {
        Set<PsiFile> depsByFile = new HashSet<PsiFile>();
        for (DependenciesBuilder builder : builders) {
          Set<PsiFile> deps = builder.getDependencies().get(psiFile);
          if (deps != null) {
            depsByFile.addAll(deps);
          }
        }
        precomputedDeps = new HashSet<PsiFile>(depsByFile);
        precomputedDeps.retainAll(searchFor);
        if (precomputedDeps.isEmpty()) continue nextFile;
      }
      else {
        precomputedDeps = Collections.unmodifiableSet(searchFor);
      }

      DependenciesBuilder.analyzeFileDependencies(psiFile, new DependenciesBuilder.DependencyProcessor() {
        @Override
        public void process(PsiElement place, PsiElement dependency) {
          PsiFile dependencyFile = dependency.getContainingFile();
          if (precomputedDeps.contains(dependencyFile)) {
            usages.add(new UsageInfo(place));
          }
        }
      });
    }

    return usages.toArray(new UsageInfo[usages.size()]);
  }

  public static UsageInfo[] findBackwardDependencies(List<DependenciesBuilder> builders, Set<PsiFile> searchIn, final Set<PsiFile> searchFor) {
    final List<UsageInfo> usages = new ArrayList<UsageInfo>();
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();


    Set<PsiFile> deps = new HashSet<PsiFile>();
    for (PsiFile psiFile : searchFor) {
      for (DependenciesBuilder builder : builders) {
        Set<PsiFile> depsByBuilder = builder.getDependencies().get(psiFile);
        if (depsByBuilder != null) {
          deps.addAll(depsByBuilder);
        }
      }
    }
    deps.retainAll(searchIn);
    if (deps.isEmpty()) return new UsageInfo[0];

    int totalCount = deps.size();
    int count = 0;
    for (PsiFile psiFile : deps) {
      count = updateIndicator(indicator, totalCount, count, psiFile);

      DependenciesBuilder.analyzeFileDependencies(psiFile, new DependenciesBuilder.DependencyProcessor() {
        @Override
        public void process(PsiElement place, PsiElement dependency) {
          PsiFile dependencyFile = dependency.getContainingFile();
          if (searchFor.contains(dependencyFile)) {
            usages.add(new UsageInfo(place));
          }
        }
      });
    }

    return usages.toArray(new UsageInfo[usages.size()]);
  }

  private static int updateIndicator(ProgressIndicator indicator, int totalCount, int count, PsiFile psiFile) {
    if (indicator != null) {
      if (indicator.isCanceled()) throw new ProcessCanceledException();
      indicator.setFraction(((double)++count) / totalCount);
      VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null) {
        indicator.setText(AnalysisScopeBundle.message("find.dependencies.progress.text", virtualFile.getPresentableUrl()));
      }
    }
    return count;
  }

  public static UsageInfo[] findDependencies(DependenciesBuilder builder, Set<PsiFile> searchIn, Set<PsiFile> searchFor) {
    return findDependencies(Collections.singletonList(builder), searchIn, searchFor);
  }

  public static UsageInfo[] findBackwardDependencies(DependenciesBuilder builder, Set<PsiFile> searchIn, Set<PsiFile> searchFor) {
    return findBackwardDependencies(Collections.singletonList(builder), searchIn, searchFor);
  }
}