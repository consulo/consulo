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
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.CheckBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

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
    private CheckBox rememberPasswordCheckBox;

    /**
     * @param description      Description text above the text fields.
     * @param login            Initial login value.
     * @param password         Initial password value.
     * @param rememberPassword Default value for the 'remember password' checkbox.
     *                         If true, the checkbox will be selected; if false, the checkbox won't be selected; if null, there will be no checkbox for remembering
     *                         password.
     */
    @RequiredUIAccess
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
            rememberPasswordCheckBox.setValue(rememberPassword);
        }
    }

    public String getLogin() {
        return myLoginTextField.getText();
    }

    public char[] getPassword() {
        return myPasswordTextField.getPassword();
    }

    public boolean isRememberPassword() {
        return rememberPasswordCheckBox.getValue();
    }

    /**
     * @return the component which should be focused when the dialog appears on the screen. May be used in dialogs.
     * @see DialogWrapper#getPreferredFocusedComponent()
     */
    public JComponent getPreferredFocusedComponent() {
        return getLogin().isEmpty() ? myLoginTextField : myPasswordTextField;
    }

    @RequiredUIAccess
    private void $$$setupUI$$$() {
        myMainPanel = new JPanel();
        myMainPanel.setLayout(new GridLayoutManager(4, 2, JBUI.insets(10), -1, -1));
        myDescriptionLabel = new JLabel();
        myDescriptionLabel.setText("##");
        myMainPanel.add(myDescriptionLabel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Label label1 = Label.create(CommonLocalize.editboxPassword());
        myMainPanel.add(TargetAWT.to(label1), new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myPasswordTextField = new JPasswordField();
        myMainPanel.add(myPasswordTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        myLoginTextField = new JTextField();
        myMainPanel.add(myLoginTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        Label label2 = Label.create(CommonLocalize.editboxLogin());
        myMainPanel.add(TargetAWT.to(label2), new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rememberPasswordCheckBox = CheckBox.create(CommonLocalize.checkboxRememberPassword());
        myMainPanel.add(TargetAWT.to(rememberPasswordCheckBox), new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    public JComponent $$$getRootComponent$$$() {
        return myMainPanel;
    }
}
