/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.refactoring.rename;

import consulo.codeEditor.Editor;
import consulo.application.ui.NonFocusableSetting;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.RenameDialog;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public abstract class RenameWithOptionalReferencesDialog extends RenameDialog {
  private CheckBox myCbSearchForReferences;

  public RenameWithOptionalReferencesDialog(@Nonnull Project project,
                                            @Nonnull PsiElement psiElement,
                                            @Nullable PsiElement nameSuggestionContext,
                                            Editor editor) {
    super(project, psiElement, nameSuggestionContext, editor);
  }

  @Override
  @RequiredUIAccess
  protected void createCheckboxes(JPanel panel, GridBagConstraints gbConstraints) {
    gbConstraints.insets = JBUI.insetsBottom(4);
    gbConstraints.gridwidth = 1;
    gbConstraints.gridx = 0;
    gbConstraints.weighty = 0;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;
    myCbSearchForReferences = CheckBox.create(RefactoringLocalize.searchForReferences());
    NonFocusableSetting.initFocusability(myCbSearchForReferences);
    myCbSearchForReferences.setValue(getSearchForReferences());
    panel.add(TargetAWT.to(myCbSearchForReferences), gbConstraints);

    super.createCheckboxes(panel, gbConstraints);
  }

  @Override
  protected void doAction() {
    setSearchForReferences(myCbSearchForReferences.getValue());
    super.doAction();
  }

  protected abstract boolean getSearchForReferences();

  protected abstract void setSearchForReferences(boolean value);
}
