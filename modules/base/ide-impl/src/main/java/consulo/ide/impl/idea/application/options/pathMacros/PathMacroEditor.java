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
package consulo.ide.impl.idea.application.options.pathMacros;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.application.HelpManager;
import consulo.application.localize.ApplicationLocalize;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @author dsl
 */
public class PathMacroEditor extends DialogWrapper {
    private TextBox myNameField;
    private JPanel myPanel;
    private TextFieldWithBrowseButton myValueField;
    private final Validator myValidator;

    private void createUIComponents() {
        myPanel = new JPanel();
        myPanel.setLayout(new GridLayoutManager(1, 2, JBUI.emptyInsets(), -1, -1));
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, JBUI.emptyInsets(), -1, -1));
        myPanel.add(
            panel1,
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myNameField = TextBox.create();
        panel1.add(
            TargetAWT.to(myNameField),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                new Dimension(100, -1),
                null,
                0,
                false
            )
        );
        myValueField = new TextFieldWithBrowseButton();
        panel1.add(
            myValueField,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                new Dimension(200, -1),
                null,
                0,
                false
            )
        );
        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, JBUI.emptyInsets(), -1, -1));
        myPanel.add(
            panel2,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );

        panel2.add(
            TargetAWT.to(Label.create(ApplicationLocalize.editboxPathMacroName())),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        panel2.add(
            TargetAWT.to(Label.create(ApplicationLocalize.editboxPathMacroValue())),
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
    }

    public interface Validator {
        boolean checkName(String name);

        boolean isOK(String name, String value);
    }

    @RequiredUIAccess
    public PathMacroEditor(String title, String macroName, String value, Validator validator) {
        super(true);
        createUIComponents();
        setTitle(title);
        myValidator = validator;
        myNameField.setValue(macroName);
        myNameField.addValueListener(event -> updateControls());
        myValueField.setText(value);
        myValueField.addBrowseFolderListener(
            LocalizeValue.empty(),
            LocalizeValue.empty(),
            null,
            new FileChooserDescriptor(false, true, true, false, true, false),
            new TextComponentAccessor<>() {
                @Override
                public String getText(JTextField component) {
                    return component.getText();
                }

                @Override
                public void setText(JTextField component, String text) {
                    int len = text.length();
                    if (len > 0 && text.charAt(len - 1) == File.separatorChar) {
                        text = text.substring(0, len - 1);
                    }
                    component.setText(text);
                }
            }
        );
        myValueField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            public void textChanged(DocumentEvent event) {
                updateControls();
            }
        });

        init();
        updateControls();
    }

    public void setMacroNameEditable(boolean isEditable) {
        myNameField.setEditable(isEditable);
    }

    private void updateControls() {
        boolean isNameOK = myValidator.checkName(myNameField.getValue());
        getOKAction().setEnabled(isNameOK);
        if (isNameOK) {
            String text = myValueField.getText().trim();
            getOKAction().setEnabled(text.length() > 0 && !"/".equals(text.trim()));
        }
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return (JComponent) TargetAWT.to(myNameField);
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
    }

    @Override
    @RequiredUIAccess
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(PathMacroConfigurable.ID);
    }

    @Override
    protected void doOKAction() {
        if (!myValidator.isOK(getName(), getValue())) {
            return;
        }
        super.doOKAction();
    }

    public String getName() {
        return myNameField.getValue().trim();
    }

    public String getValue() {
        String path = myValueField.getText().trim();
        File file = new File(path);
        if (file.isAbsolute()) {
            try {
                return file.getCanonicalPath();
            }
            catch (IOException ignored) {
            }
        }
        return path;
    }

    @Override
    protected JComponent createNorthPanel() {
        return myPanel;
    }

    @Override
    protected JComponent createCenterPanel() {
        return null;
    }
}
