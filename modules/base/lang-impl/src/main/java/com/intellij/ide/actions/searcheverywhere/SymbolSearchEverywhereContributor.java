// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.util.gotoByName.FilteringGotoByModel;
import com.intellij.ide.util.gotoByName.GotoSymbolModel2;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.ui.IdeUICustomization;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class SymbolSearchEverywhereContributor extends AbstractGotoSEContributor {

  private final PersistentSearchEverywhereContributorFilter<Language> myFilter;

  public SymbolSearchEverywhereContributor(@Nullable Project project, @Nullable PsiElement context) {
    super(project, context);
    myFilter = project == null ? null : ClassSearchEverywhereContributor.createLanguageFilter(project);
  }

  @Nonnull
  @Override
  public String getGroupName() {
    return "Symbols";
  }

  @Nonnull
  public String includeNonProjectItemsText() {
    return IdeBundle.message("checkbox.include.non.project.symbols", IdeUICustomization.getInstance().getProjectConceptName());
  }

  @Override
  public int getSortWeight() {
    return 300;
  }

  @Nonnull
  @Override
  protected FilteringGotoByModel<Language> createModel(@Nonnull Project project) {
    GotoSymbolModel2 model = new GotoSymbolModel2(project);
    if (myFilter != null) {
      model.setFilterItems(myFilter.getSelectedElements());
    }
    return model;
  }

  @Nonnull
  @Override
  public List<AnAction> getActions(@Nonnull Runnable onChanged) {
    return doGetActions(includeNonProjectItemsText(), myFilter, onChanged);
  }

  public static class Factory implements SearchEverywhereContributorFactory<Object> {
    @Nonnull
    @Override
    public SearchEverywhereContributor<Object> createContributor(@Nonnull AnActionEvent initEvent) {
      return new SymbolSearchEverywhereContributor(initEvent.getProject(), GotoActionBase.getPsiContext(initEvent));
    }
  }
}
