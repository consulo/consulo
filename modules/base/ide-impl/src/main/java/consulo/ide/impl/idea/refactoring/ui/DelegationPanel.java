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
package consulo.ide.impl.idea.refactoring.ui;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;

import javax.swing.*;

/**
 * @author dsl
 */
public class DelegationPanel extends JPanel {
  private final JRadioButton myRbModifyCalls;
  private final JRadioButton myRbGenerateDelegate;

  public DelegationPanel() {
    final BoxLayout boxLayout = new BoxLayout(this, BoxLayout.X_AXIS);
    setLayout(boxLayout);
    add(new JLabel(RefactoringLocalize.delegationPanelMethodCallsLabel().get()));
    myRbModifyCalls = new JRadioButton();
    myRbModifyCalls.setText(RefactoringLocalize.delegationPanelModifyRadio().get());
    add(myRbModifyCalls);
    myRbGenerateDelegate = new JRadioButton();
    myRbGenerateDelegate.setText(RefactoringLocalize.delegationPanelDelegateViaOverloadingMethod().get());
    add(myRbGenerateDelegate);
    myRbModifyCalls.setSelected(true);
    final ButtonGroup bg = new ButtonGroup();
    bg.add(myRbModifyCalls);
    bg.add(myRbGenerateDelegate);
    add(Box.createHorizontalGlue());
    myRbModifyCalls.addItemListener(e -> stateModified());
    myRbGenerateDelegate.addItemListener(e -> stateModified());
  }

  protected void stateModified() {
  }

  public boolean isModifyCalls() {
    return myRbModifyCalls.isSelected();
  }

  public boolean isGenerateDelegate() {
    return myRbGenerateDelegate.isSelected();
  }
}
