// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.ui.internal.scope;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.internal.ModelScopeItem;
import consulo.language.editor.internal.ModelScopeItemPresenter;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.psi.PsiElement;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.RadioButton;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "project_scope", order = "first")
public final class ProjectScopeItemPresenter implements ModelScopeItemPresenter {
  @Override
  public int getScopeId() {
    return AnalysisScope.PROJECT;
  }

  @Override
  @Nonnull
  public RadioButton getButton(ModelScopeItem m) {
    return RadioButton.create(AnalysisScopeLocalize.scopeOptionWholeProject());
  }

  @Override
  public boolean isApplicable(ModelScopeItem model) {
    return model instanceof ProjectScopeItem;
  }

  @Override
  public @Nonnull ModelScopeItem tryCreate(@Nonnull Project project,
                                           @Nonnull AnalysisScope scope,
                                           @Nullable Module module,
                                           @Nullable PsiElement context) {
    return new ProjectScopeItem(project);
  }
}