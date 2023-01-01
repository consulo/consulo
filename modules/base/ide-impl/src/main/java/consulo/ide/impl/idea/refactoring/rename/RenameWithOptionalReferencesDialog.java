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
import consulo.language.editor.refactoring.rename.RenameDialog;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.ui.ex.awt.NonFocusableCheckBox;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public abstract class RenameWithOptionalReferencesDialog extends RenameDialog {
  private JCheckBox myCbSearchForReferences;

  public RenameWithOptionalReferencesDialog(@Nonnull Project project,
                                            @Nonnull PsiElement psiElement,
                                            @Nullable PsiElement nameSuggestionContext,
                                            Editor editor) {
    super(project, psiElement, nameSuggestionContext, editor);
  }

  @Override
  protected void createCheckboxes(JPanel panel, GridBagConstraints gbConstraints) {
    gbConstraints.insets = new Insets(0, 0, 4, 0);
    gbConstraints.gridwidth = 1;
    gbConstraints.gridx = 0;
    gbConstraints.weighty = 0;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;
    myCbSearchForReferences = new NonFocusableCheckBox(RefactoringBundle.message("search.for.references"));
    myCbSearchForReferences.setSelected(getSearchForReferences());
    panel.add(myCbSearchForReferences, gbConstraints);

    super.createCheckboxes(panel, gbConstraints);
  }

  @Override
  protected void doAction() {
    setSearchForReferences(myCbSearchForReferences.isSelected());
    super.doAction();
  }

  protected abstract boolean getSearchForReferences();

  protected abstract void setSearchForReferences(boolean value);
}
