/*
 * Copyright 2013-2017 consulo.io
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
package consulo.auth.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import consulo.auth.ServiceAuthConfiguration;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;

/**
 * @author VISTALL
 * @since 06-Mar-17
 */
public class ServiceAuthDialog extends DialogWrapper {
  private JPanel myRoot;

  private final ServiceAuthConfiguration myServiceAuthConfiguration;
  private final JRadioButton myAsAnonymousButton;
  private final JRadioButton myLogAsButton;
  private final JBTextField myEmailField;

  public ServiceAuthDialog() {
    super(null);
    setTitle("Service Authorization");
    myRoot = new JPanel(new VerticalFlowLayout(0, 0));

    myServiceAuthConfiguration = ServiceAuthConfiguration.getInstance();

    myAsAnonymousButton = new JBRadioButton("Logged as anonymous");
    myLogAsButton = new JBRadioButton("Log as user");

    JPanel loginPanel = new JPanel(new VerticalFlowLayout(0, 0));
    myEmailField = new JBTextField();
    loginPanel.add(LabeledComponent.left(myEmailField, "Email"));

    ButtonGroup group = new ButtonGroup();
    group.add(myAsAnonymousButton);
    group.add(myLogAsButton);

    myLogAsButton.addItemListener(e -> UIUtil.setEnabled(loginPanel, e.getStateChange() == ItemEvent.SELECTED, true));

    myRoot.add(myAsAnonymousButton);
    myRoot.add(myLogAsButton);
    myRoot.add(loginPanel);

    String email = myServiceAuthConfiguration.getEmail();
    if (email == null) {
      myAsAnonymousButton.setSelected(true);
    }
    else {
      myLogAsButton.setSelected(true);
      myEmailField.setText(email);
    }

    pack();
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myRoot;
  }

  @Override
  protected void doOKAction() {
    if (myAsAnonymousButton.isSelected()) {
      myServiceAuthConfiguration.setEmail(null);
    }
    else {
      myServiceAuthConfiguration.setEmail(myEmailField.getText());
    }

    myServiceAuthConfiguration.updateIcon();

    super.doOKAction();
  }
}
