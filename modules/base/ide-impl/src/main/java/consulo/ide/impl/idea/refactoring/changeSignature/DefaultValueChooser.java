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
package consulo.ide.impl.idea.refactoring.changeSignature;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.language.editor.ui.awt.RadioUpDownListener;
import consulo.language.editor.ui.awt.EditorTextField;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author anna
 * @since 2010-09-13
 */
public class DefaultValueChooser extends DialogWrapper{
  private JRadioButton myLeaveBlankRadioButton;
  private JRadioButton myFeelLuckyRadioButton;
  private JLabel myFeelLuckyDescription;
  private JRadioButton myUseValueRadioButton;
  private EditorTextField myValueEditor;
  private JPanel myWholePanel;
  private JLabel myBlankDescription;

  public DefaultValueChooser(Project project, String name, String defaultValue) {
    super(project);
    new RadioUpDownListener(myLeaveBlankRadioButton, myFeelLuckyRadioButton, myUseValueRadioButton);
    final ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myValueEditor.setEnabled(myUseValueRadioButton.isSelected());
        if (myUseValueRadioButton.isSelected()) {
          myValueEditor.selectAll();
          IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myValueEditor);
        }
      }
    };
    myLeaveBlankRadioButton.addActionListener(actionListener);
    myFeelLuckyRadioButton.addActionListener(actionListener);
    myUseValueRadioButton.addActionListener(actionListener);
    setTitle("Default value for parameter \"" + name + "\" needed");
    myLeaveBlankRadioButton.setSelected(true);
    myValueEditor.setEnabled(false);
    myFeelLuckyDescription.setText("Variables of the same type would be searched in the method call place.\n" +
                                   "When exactly one variable is found, it would be used.\n" +
                                   "Otherwise parameter place would be left blank.");
    myFeelLuckyDescription.setUI(new MultiLineLabelUI());
    myBlankDescription.setUI(new MultiLineLabelUI());
    myValueEditor.setText(defaultValue);
    init();
  }

  public boolean feelLucky() {
    return myFeelLuckyRadioButton.isSelected();
  }


  public String getDefaultValue() {
    if (myLeaveBlankRadioButton.isSelected()) {
      return "";
    }
    return myValueEditor.getText();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myLeaveBlankRadioButton;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myWholePanel;
  }
}
