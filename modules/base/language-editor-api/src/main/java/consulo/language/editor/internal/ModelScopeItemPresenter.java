/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.language.editor.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.disposer.Disposable;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiElement;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ModelScopeItemPresenter {
  ExtensionPointName<ModelScopeItemPresenter> EP_NAME = ExtensionPointName.create(ModelScopeItemPresenter.class);

  @AnalysisScope.Type
  int getScopeId();

  @Nonnull
  RadioButton getButton(ModelScopeItem model);

  @Nullable
  @RequiredUIAccess
  default Component getAdditionalComponents(RadioButton button, ModelScopeItem model, Disposable dialogDisposable) {
    return null;
  }

  boolean isApplicable(ModelScopeItem model);

  @Nullable
  default ModelScopeItem tryCreate(@Nonnull Project project,
                                   @Nonnull AnalysisScope scope,
                                   @Nullable Module module,
                                   @Nullable PsiElement context) {
    return null;
  }

  @Nonnull
  @RequiredUIAccess
  static List<ModelScopeItemView> createOrderedViews(List<? extends ModelScopeItem> models, Disposable dialogDisposable) {
    List<ModelScopeItemView> result = new ArrayList<>();
    for (ModelScopeItemPresenter presenter : EP_NAME.getExtensionList()) {
      for (ModelScopeItem model : models) {
        if (presenter.isApplicable(model)) {
          RadioButton button = presenter.getButton(model);
          Component additionalComponent = presenter.getAdditionalComponents(button, model, dialogDisposable);
          int id = presenter.getScopeId();
          result.add(new ModelScopeItemView(button, additionalComponent, model, id));
          break;
        }
      }
    }
    return result;
  }
}