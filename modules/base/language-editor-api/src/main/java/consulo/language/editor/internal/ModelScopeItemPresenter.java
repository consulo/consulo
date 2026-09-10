/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.language.editor.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.RadioGroup;
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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ModelScopeItemPresenter {
  ExtensionPointName<ModelScopeItemPresenter> EP_NAME = ExtensionPointName.create(ModelScopeItemPresenter.class);

  @AnalysisScope.Type
  int getScopeId();

  
  /**
   * The label of this option. The button itself is made by the group which owns every option of the dialog, so
   * that choosing one unchooses the rest.
   */
  LocalizeValue getButtonText(ModelScopeItem model);

  @RequiredUIAccess
  default @Nullable Component getAdditionalComponents(RadioButton button, ModelScopeItem model, Disposable dialogDisposable) {
    return null;
  }

  boolean isApplicable(ModelScopeItem model);

  default @Nullable ModelScopeItem tryCreate(Project project,
                                   AnalysisScope scope,
                                   @Nullable Module module,
                                   @Nullable PsiElement context) {
    return null;
  }

  
  @RequiredUIAccess
  static List<ModelScopeItemView> createOrderedViews(RadioGroup<Integer> group,
                                                    List<? extends ModelScopeItem> models,
                                                    Disposable dialogDisposable) {
    List<ModelScopeItemView> result = new ArrayList<>();
    for (ModelScopeItemPresenter presenter : EP_NAME.getExtensionList()) {
      for (ModelScopeItem model : models) {
        if (presenter.isApplicable(model)) {
          int id = presenter.getScopeId();
          RadioButton button = group.newButton(presenter.getButtonText(model), id);
          Component additionalComponent = presenter.getAdditionalComponents(button, model, dialogDisposable);
          result.add(new ModelScopeItemView(button, additionalComponent, model, id));
          break;
        }
      }
    }
    return result;
  }
}