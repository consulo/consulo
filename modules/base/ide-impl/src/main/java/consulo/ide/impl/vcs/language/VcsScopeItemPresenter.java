// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.vcs.language;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
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
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.SimpleListCellRenderer;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.change.LocalChangeList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

@ExtensionImpl(id = "vcs_scope", order = "after module_scope")
public class VcsScopeItemPresenter implements ModelScopeItemPresenter {

  @Override
  public int getScopeId() {
    return AnalysisScope.UNCOMMITTED_FILES;
  }

  @Nonnull
  @Override
  public RadioButton getButton(ModelScopeItem m) {
    return RadioButton.create(AnalysisScopeLocalize.scopeOptionUncommittedFiles());
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public Component getAdditionalComponents(RadioButton button, ModelScopeItem m, Disposable dialogDisposable) {
    VcsScopeItem model = (VcsScopeItem)m;
    DefaultComboBoxModel<LocalChangeList> comboBoxModel = model.getChangeListsModel();
    if (comboBoxModel == null) {
      return null;
    }

    ComboBox<LocalChangeList> comboBox = new ComboBox<>();
    comboBox.setRenderer(SimpleListCellRenderer.create((@Nonnull JBLabel label, @Nullable LocalChangeList value, int index) -> {
      int availableWidth = comboBox.getWidth(); // todo, is it correct?
      if (availableWidth <= 0) {
        availableWidth = JBUIScale.scale(200);
      }

      String text = value != null ? value.getName() : AnalysisScopeLocalize.scopeOptionUncommittedFilesAllChangelistsChoice().get();
      if (label.getFontMetrics(label.getFont()).stringWidth(text) >= availableWidth) {
        text = StringUtil.trimLog(text, 50);
      }

      label.setText(text);
    }));

    comboBox.setModel(comboBoxModel);
    comboBox.setEnabled(button.getValue());
    button.addValueListener(e -> comboBox.setEnabled(button.getValueOrError()));

    return TargetAWT.wrap(comboBox);
  }

  @Override
  public boolean isApplicable(ModelScopeItem model) {
    return model instanceof VcsScopeItem;
  }

  @Override
  @Nullable
  public ModelScopeItem tryCreate(@Nonnull Project project,
                                            @Nonnull AnalysisScope scope,
                                            @Nullable Module module,
                                            @Nullable PsiElement context) {
    return VcsScopeItem.createIfHasVCS(project);
  }
}