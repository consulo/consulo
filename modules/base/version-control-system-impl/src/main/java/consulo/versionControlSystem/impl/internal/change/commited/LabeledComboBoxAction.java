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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.action.CustomComponentAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author yole
 */
public abstract class LabeledComboBoxAction extends AnAction implements CustomComponentAction {
  private final JLabel myLabel;
  private JPanel myPanel;
  private final JComboBox myComboBox;

  protected LabeledComboBoxAction(String label) {
    String labelString = label;
    myComboBox = new JComboBox();
    myLabel = new JLabel(labelString);
  }

  public void actionPerformed(AnActionEvent e) {
  }

  public JComponent createCustomComponent(Presentation presentation, String place) {
    if (myPanel == null) {
      myPanel = new JPanel(new BorderLayout());
      myPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
      myPanel.add(myLabel, BorderLayout.WEST);
      myComboBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          selectionChanged(myComboBox.getSelectedItem());
        }
      });
      myComboBox.setModel(createModel());
      myPanel.add(myComboBox, BorderLayout.CENTER);
    }
    return myPanel;
  }

  protected void setModel(ComboBoxModel model) {
    myComboBox.setModel(model);
  }

  protected void enableSelf(boolean enable) {
    myComboBox.setEnabled(enable);
    myLabel.setEnabled(enable);
  }

  protected boolean isEnabled() {
    return myComboBox.isEnabled();
  }

  protected Object getSelected() {
    return myComboBox.getSelectedItem();
  }

  protected ComboBoxModel getModel() {
    return myComboBox.getModel();
  }

  protected void setRenderer(ListCellRenderer renderer) {
    myComboBox.setRenderer(renderer);
  }

  protected abstract void selectionChanged(Object selection);

  protected abstract ComboBoxModel createModel();
  
  public void setSelected(int idx) {
    ComboBoxModel boxModel = getModel();
    if (boxModel.getSize() > 0) {
      boxModel.setSelectedItem(boxModel.getElementAt(idx));
    }
  }

  protected JComboBox getComboBox() {
    return myComboBox;
  }
}
