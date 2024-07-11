// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.ui.internal.scope;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.internal.ModelScopeItem;
import consulo.language.editor.internal.ModelScopeItemPresenter;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.RadioButton;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "other_scope", order = "after vcs_scope")
public final class OtherScopeItemPresenter implements ModelScopeItemPresenter {

  @Override
  public int getScopeId() {
    return AnalysisScope.FILE;
  }

  @Nonnull
  @Override
  public RadioButton getButton(ModelScopeItem m) {
    OtherScopeItem model = (OtherScopeItem)m;
    AnalysisScope scope = model.getScope();
    RadioButton button = RadioButton.create(LocalizeValue.of(scope.getShortenName()));
    String name = scope.getShortenName();
    // TODO button.setMnemonic(name.charAt(getSelectedScopeMnemonic(name)));
    return button;
  }

  @Override
  public boolean isApplicable(ModelScopeItem model) {
    return model instanceof OtherScopeItem;
  }

  private static int getSelectedScopeMnemonic(String name) {

    final int fileIdx = StringUtil.indexOfIgnoreCase(name, "file", 0);
    if (fileIdx > -1) {
      return fileIdx;
    }

    final int dirIdx = StringUtil.indexOfIgnoreCase(name, "directory", 0);
    if (dirIdx > -1) {
      return dirIdx;
    }

    return 0;
  }

  @Override
  @Nullable
  public ModelScopeItem tryCreate(@Nonnull Project project,
                                  @Nonnull AnalysisScope scope,
                                  @Nullable Module module,
                                  @Nullable PsiElement context) {
    return OtherScopeItem.tryCreate(scope);
  }
}