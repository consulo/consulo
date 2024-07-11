// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.ui.internal.scope;

import consulo.language.editor.internal.ModelScopeItem;
import consulo.language.editor.scope.AnalysisScope;
import consulo.project.Project;

public final class ProjectScopeItem implements ModelScopeItem {
  public final Project project;

  public ProjectScopeItem(Project project) {
    this.project = project;
  }

  @Override
  public AnalysisScope getScope() {
    return new AnalysisScope(project);
  }
}