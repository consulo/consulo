/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.credentialStorage.impl.internal.ui;

import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.DialogUtil;
import consulo.util.lang.StringUtil;

import javax.swing.*;

/**
 * @author gregsh
 */
public class PasswordPromptComponent {
    private JPanel myRootPanel;
    private JPanel myUserPanel;
    private JPanel myPasswordPanel;
    private JPasswordField myPasswordField;
    private JCheckBox myRememberCheckBox;
    private JLabel myMessageLabel;
    private JLabel myPasswordLabel;
    private JLabel myUserLabel;
    private JTextField myUserTextField;

    public PasswordPromptComponent(boolean memoryOnly,
                                   String message,
                                   boolean showUserName,
                                   String passwordPrompt,
                                   String rememberPrompt) {
        myMessageLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
        myMessageLabel.setText(message);

        if (memoryOnly) {
            myRememberCheckBox.setVisible(false);
            myRememberCheckBox.setEnabled(false);
            myRememberCheckBox.setSelected(false);
        }
        else {
            myRememberCheckBox.setEnabled(true);
            myRememberCheckBox.setSelected(true);
        }

        setUserInputVisible(showUserName);

        if (passwordPrompt != null) {
            myPasswordLabel.setText(passwordPrompt);
        }

        if (rememberPrompt != null) {
            myRememberCheckBox.setText(rememberPrompt);
            DialogUtil.registerMnemonic(myRememberCheckBox);
        }
    }

    public JComponent getComponent() {
        return myRootPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return myUserTextField.isEnabled() && StringUtil.isEmpty(myUserTextField.getText()) ? myUserTextField : myPasswordField;
    }

    private void setUserInputVisible(boolean visible) {
        UIUtil.setEnabled(myUserPanel, visible, true);
        myUserPanel.setVisible(visible);
    }

    public String getUserName() {
        return myUserTextField.getText();
    }

    public void setUserName(String text) {
        myUserTextField.setText(text);
    }

    public char[] getPassword() {
        return myPasswordField.getPassword();
    }

    public void setPassword(String text) {
        myPasswordField.setText(text);
    }

    public boolean isRememberSelected() {
        return myRememberCheckBox.isSelected();
    }

    public void setRememberSelected(boolean selected) {
        myRememberCheckBox.setSelected(selected);
    }
}
