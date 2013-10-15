/*
 * Copyright 2013 Consulo.org
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
package org.consulo.ide.eap;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Function;
import org.jdesktop.swingx.HorizontalLayout;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.Arrays;

/**
 * @author VISTALL
 * @since 16:30/15.10.13
 */
public class EarlyAccessProgramConfigurable implements Configurable {
  private CheckBoxList<EarlyAccessProgramDescriptor> myList;

  @Nls
  @Override
  public String getDisplayName() {
    return IdeBundle.message("eap.configurable.name");
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    JPanel warningPanel = new JPanel(new HorizontalLayout()){
      @Override
      public Color getBackground() {
        return JBColor.YELLOW;
      }
    };
    warningPanel.setBorder(BorderFactory.createLineBorder(JBColor.ORANGE, 1));

    JPanel mainPanel = new JPanel(new VerticalFlowLayout(true, true));
    warningPanel.add(new JBLabel(AllIcons.General.WarningDialog));
    warningPanel.add(new JBLabel(IdeBundle.message("eap.configurable.warning")));

    mainPanel.add(warningPanel);

    myList = new CheckBoxList<EarlyAccessProgramDescriptor>();
    myList
      .setItems(Arrays.asList(EarlyAccessProgramDescriptor.EP_NAME.getExtensions()), new Function<EarlyAccessProgramDescriptor, String>() {
                  @Override
                  public String fun(EarlyAccessProgramDescriptor earlyAccessProgramDescriptor) {
                    return earlyAccessProgramDescriptor.getName();
                  }
                }, new Function<EarlyAccessProgramDescriptor, Boolean>() {
                  @Override
                  public Boolean fun(EarlyAccessProgramDescriptor earlyAccessProgramDescriptor) {
                    return EarlyAccessProgramManager.getInstance().getState(earlyAccessProgramDescriptor.getClass());
                  }
                }
      );

    final JBSplitter splitter = new JBSplitter(false, 0.3f);
    splitter.setSplitterProportionKey("#EarlyAccessProgramConfigurable");
    splitter.setFirstComponent(myList);

    final JTextArea textField = new JTextArea();
    textField.setEditable(false);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(textField , BorderLayout.NORTH);

    splitter.setSecondComponent(panel);
    myList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        Object itemAt = myList.getItemAt(e.getFirstIndex());
        if (itemAt instanceof EarlyAccessProgramDescriptor) {
          textField.setText(((EarlyAccessProgramDescriptor)itemAt).getDescription());
        }
        else {
          textField.setText(null);
        }
      }
    });

    mainPanel.add(splitter);
    return mainPanel;
  }

  @Override
  public boolean isModified() {
    EarlyAccessProgramManager manager = EarlyAccessProgramManager.getInstance();

    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensions()) {
      if (myList.isItemSelected(descriptor) != manager.getState(descriptor.getClass())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    EarlyAccessProgramManager manager = EarlyAccessProgramManager.getInstance();

    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensions()) {
      manager.setState(descriptor.getClass(), myList.isItemSelected(descriptor));
    }
  }

  @Override
  public void reset() {
    EarlyAccessProgramManager manager = EarlyAccessProgramManager.getInstance();

    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensions()) {
      myList.setItemSelected(descriptor, manager.getState(descriptor.getClass()));
    }
  }

  @Override
  public void disposeUIResources() {
  }
}
