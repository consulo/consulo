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

package consulo.ide.impl.idea.packageDependencies.actions;

import consulo.ide.impl.idea.packageDependencies.BackwardDependenciesBuilder;
import consulo.ide.impl.idea.packageDependencies.DependenciesBuilder;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author anna
 * @since 2005-01-16
 */
public class BackwardDependenciesHandler extends DependenciesHandlerBase {
  private final AnalysisScope myScopeOfInterest;

  public BackwardDependenciesHandler(Project project, AnalysisScope scope, final AnalysisScope selectedScope) {
    this(project, Collections.singletonList(scope), selectedScope, new HashSet<>());
  }

  public BackwardDependenciesHandler(final Project project, final List<AnalysisScope> scopes, final @Nullable AnalysisScope scopeOfInterest, Set<PsiFile> excluded) {
    super(project, scopes, excluded);
    myScopeOfInterest = scopeOfInterest;
  }

  @Override
  protected String getProgressTitle() {
    return AnalysisScopeLocalize.backwardDependenciesProgressText().get();
  }

  @Override
  protected String getPanelDisplayName(final AnalysisScope scope) {
    return AnalysisScopeLocalize.backwardDependenciesToolwindowTitle(scope.getDisplayName()).get();
  }

  @Override
  protected DependenciesBuilder createDependenciesBuilder(AnalysisScope scope) {
    return new BackwardDependenciesBuilder(myProject, scope, myScopeOfInterest);
  }
}
