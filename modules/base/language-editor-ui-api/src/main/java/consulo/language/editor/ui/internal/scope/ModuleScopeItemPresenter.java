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
import org.jspecify.annotations.Nullable;

@ExtensionImpl(id = "module_scope", order = "after project_scope")
public final class ModuleScopeItemPresenter implements ModelScopeItemPresenter {
  @Override
  public int getScopeId() {
    return AnalysisScope.MODULE;
  }

  @Override
  
  public RadioButton getButton(ModelScopeItem m) {
    ModuleScopeItem model = (ModuleScopeItem)m;
    return RadioButton.create(AnalysisScopeLocalize.scopeOptionModuleWithMnemonic(model.Module.getName()));
  }

  @Override
  public boolean isApplicable(ModelScopeItem model) {
    return model instanceof ModuleScopeItem;
  }

  @Override
  public @Nullable ModelScopeItem tryCreate(Project project,
                                  AnalysisScope scope,
                                  @Nullable Module module,
                                  @Nullable PsiElement context) {
    return ModuleScopeItem.tryCreate(module);
  }
}