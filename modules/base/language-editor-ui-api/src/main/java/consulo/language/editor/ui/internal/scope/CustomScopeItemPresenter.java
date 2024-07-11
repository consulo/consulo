// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.ui.internal.scope;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.find.ui.ScopeChooserCombo;
import consulo.language.editor.internal.ModelScopeItem;
import consulo.language.editor.internal.ModelScopeItemPresenter;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.psi.PsiElement;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "custom_scope", order = "after other_scope")
public final class CustomScopeItemPresenter implements ModelScopeItemPresenter {

  @Override
  public int getScopeId() {
    return AnalysisScope.CUSTOM;
  }

  @Nonnull
  @Override
  public RadioButton getButton(ModelScopeItem model) {
    return RadioButton.create(AnalysisScopeLocalize.scopeOptionCustom());
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Component getAdditionalComponents(RadioButton button, ModelScopeItem m, Disposable dialogDisposable) {
    CustomScopeItem model = (CustomScopeItem)m;
    ScopeChooserCombo scopeCombo = new ScopeChooserCombo();
    Disposer.register(dialogDisposable, scopeCombo);
    scopeCombo.init(model.getProject(), model.getSearchInLibFlag(), true, model.getPreselectedCustomScope(), null);
    scopeCombo.setCurrentSelection(false);
    scopeCombo.setEnabled(button.getValueOrError());
    model.setSearchScopeSupplier(() -> scopeCombo.getSelectedScope());
    button.addValueListener(e -> scopeCombo.setEnabled(button.getValue()));

    return TargetAWT.wrap(scopeCombo);
  }

  @Override
  public boolean isApplicable(ModelScopeItem model) {
    return model instanceof CustomScopeItem;
  }

  @Override
  @Nullable
  public ModelScopeItem tryCreate(@Nonnull Project project,
                                  @Nonnull AnalysisScope scope,
                                  @Nullable Module module,
                                  @Nullable PsiElement context) {
    return new CustomScopeItem(project, context);
  }
}