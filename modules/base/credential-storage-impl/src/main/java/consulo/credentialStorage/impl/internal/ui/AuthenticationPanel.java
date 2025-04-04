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
package consulo.credentialStorage.impl.internal.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.application.CommonBundle;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for authentication - contains text fields for login and password, and a checkbox to remember
 * password in the Password Safe.
 *
 * @author stathik
 * @author Kirill Likhodedov
 */
public class AuthenticationPanel extends JPanel {
    private JPanel myMainPanel;
    private JLabel myDescriptionLabel;
    private JTextField myLoginTextField;
    private JPasswordField myPasswordTextField;
    private JCheckBox rememberPasswordCheckBox;

    /**
     * @param description      Description text above the text fields.
     * @param login            Initial login value.
     * @param password         Initial password value.
     * @param rememberPassword Default value for the 'remember password' checkbox.
     *                         If true, the checkbox will be selected; if false, the checkbox won't be selected; if null, there will be no checkbox for remembering
     *                         password.
     */
    public AuthenticationPanel(@Nullable String description, @Nullable String login, @Nullable String password, @Nullable Boolean rememberPassword) {
        $$$setupUI$$$();

        add(myMainPanel);
        myDescriptionLabel.setText(description);
        myLoginTextField.setText(login);
        myPasswordTextField.setText(password);
        if (rememberPassword == null) {
            rememberPasswordCheckBox.setVisible(false);
        }
        else {
            rememberPasswordCheckBox.setSelected(rememberPassword);
        }
    }

    public String getLogin() {
        return myLoginTextField.getText();
    }

    public char[] getPassword() {
        return myPasswordTextField.getPassword();
    }

    public boolean isRememberPassword() {
        return rememberPasswordCheckBox.isSelected();
    }

    /**
     * @return the component which should be focused when the dialog appears on the screen. May be used in dialogs.
     * @see DialogWrapper#getPreferredFocusedComponent()
     */
    public JComponent getPreferredFocusedComponent() {
        return getLogin().isEmpty() ? myLoginTextField : myPasswordTextField;
    }

    private void $$$setupUI$$$() {
        myMainPanel = new JPanel();
        myMainPanel.setLayout(new GridLayoutManager(4, 2, JBUI.insets(10), -1, -1));
        myDescriptionLabel = new JLabel();
        myDescriptionLabel.setText("##");
        myMainPanel.add(myDescriptionLabel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, CommonBundle.message("editbox.password"));
        myMainPanel.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myPasswordTextField = new JPasswordField();
        myMainPanel.add(myPasswordTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        myLoginTextField = new JTextField();
        myMainPanel.add(myLoginTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, CommonBundle.message("editbox.login"));
        myMainPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rememberPasswordCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(rememberPasswordCheckBox, CommonBundle.message("checkbox.remember.password"));
        myMainPanel.add(rememberPasswordCheckBox, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myMainPanel;
    }
}
