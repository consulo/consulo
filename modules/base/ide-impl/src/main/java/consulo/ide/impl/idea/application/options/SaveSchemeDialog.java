/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options;

import consulo.application.localize.ApplicationLocalize;
import consulo.component.util.text.UniqueNameGenerator;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Yura Cangea
 */
public class SaveSchemeDialog extends DialogWrapper {
    private final JTextField mySchemeName = new JTextField();
    private final List<String> myExistingNames;

    public SaveSchemeDialog(@Nonnull Component parent, String title, @Nonnull List<String> existingNames, @Nonnull String selectedName) {
        super(parent, false);
        myExistingNames = existingNames;
        setTitle(title);
        mySchemeName.setText(UniqueNameGenerator.generateUniqueName(selectedName + " copy", existingNames));
        init();
    }

    public String getSchemeName() {
        return mySchemeName.getText();
    }

    @Override
    protected JComponent createNorthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0;
        gc.insets = JBUI.insets(5, 0, 5, 5);
        panel.add(TargetAWT.to(Label.create(ApplicationLocalize.labelName())), gc);

        gc = new GridBagConstraints();
        gc.gridx = 1;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridwidth = 2;
        gc.insets = JBUI.insetsBottom(5);
        panel.add(mySchemeName, gc);

        panel.setPreferredSize(JBUI.size(220, 40));
        return panel;
    }

    @Override
    @RequiredUIAccess
    protected void doOKAction() {
        if (getSchemeName().trim().isEmpty()) {
            Messages.showMessageDialog(
                getContentPane(),
                ApplicationLocalize.errorSchemeMustHaveAName().get(),
                CommonLocalize.titleError().get(),
                UIUtil.getErrorIcon()
            );
            return;
        }
        else if ("default".equals(getSchemeName())) {
            Messages.showMessageDialog(
                getContentPane(),
                ApplicationLocalize.errorIllegalSchemeName().get(),
                CommonLocalize.titleError().get(),
                UIUtil.getErrorIcon()
            );
            return;
        }
        else if (myExistingNames.contains(getSchemeName())) {
            Messages.showMessageDialog(
                getContentPane(),
                ApplicationLocalize.errorASchemeWithThisNameAlreadyExistsOrWasDeletedWithoutApplyingTheChanges().get(),
                CommonLocalize.titleError().get(),
                UIUtil.getErrorIcon()
            );
            return;
        }
        super.doOKAction();
    }

    @Override
    protected JComponent createCenterPanel() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return mySchemeName;
    }
}
