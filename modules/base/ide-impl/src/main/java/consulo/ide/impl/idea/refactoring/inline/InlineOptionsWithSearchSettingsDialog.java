/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.refactoring.inline;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public abstract class InlineOptionsWithSearchSettingsDialog extends InlineOptionsDialog {
  protected JCheckBox myCbSearchInComments;
  protected JCheckBox myCbSearchTextOccurences;

  protected InlineOptionsWithSearchSettingsDialog(Project project, boolean canBeParent, PsiElement element) {
    super(project, canBeParent, element);
  }

  protected abstract boolean isSearchInCommentsAndStrings();
  protected abstract void saveSearchInCommentsAndStrings(boolean searchInComments);
  
  protected abstract boolean isSearchForTextOccurrences();
  protected abstract void saveSearchInTextOccurrences(boolean searchInTextOccurrences);

  @Override
  protected void doAction() {
    final boolean searchInNonJava = myCbSearchTextOccurences.isSelected();
    final boolean searchInComments = myCbSearchInComments.isSelected();
    if (myCbSearchInComments.isEnabled() ) {
      saveSearchInCommentsAndStrings(searchInComments);
    }
    if (myCbSearchTextOccurences.isEnabled()) {
      saveSearchInTextOccurrences(searchInNonJava);
    }
  }

  public void setEnabledSearchSettngs(boolean enabled) {
    myCbSearchInComments.setEnabled(enabled);
    myCbSearchTextOccurences.setEnabled(enabled);
    if (enabled) {
      myCbSearchInComments.setSelected(isSearchInCommentsAndStrings());
      myCbSearchTextOccurences.setSelected(isSearchForTextOccurrences());
    } else {
      myCbSearchInComments.setSelected(false);
      myCbSearchTextOccurences.setSelected(false);
    }
  }

  @Override
  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridwidth = 2;
    panel.add(super.createCenterPanel(), gbc);


    myCbSearchInComments = new JCheckBox(RefactoringLocalize.searchInCommentsAndStrings().get(), isSearchInCommentsAndStrings());
    myCbSearchTextOccurences = new JCheckBox(RefactoringLocalize.searchForTextOccurrences().get(), isSearchForTextOccurrences());
    gbc.weightx = 0;
    gbc.gridwidth = 1;
    gbc.gridy = 1;
    gbc.gridx = 0;
    panel.add(myCbSearchInComments, gbc);
    gbc.gridx = 1;
    panel.add(myCbSearchTextOccurences, gbc);
    final ActionListener actionListener = e -> setEnabledSearchSettngs(myRbInlineAll.isSelected());
    myRbInlineThisOnly.addActionListener(actionListener);
    myRbInlineAll.addActionListener(actionListener);
    setEnabledSearchSettngs(myRbInlineAll.isSelected());
    return panel;
  }
}
