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

import consulo.fileChooser.FileChooserDescriptor;
import consulo.application.HelpManager;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awt.event.DocumentAdapter;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.io.IOException;

/**
 * @author dsl
 */
public class PathMacroEditor extends DialogWrapper {
    private JTextField myNameField;
    private JPanel myPanel;
    private TextFieldWithBrowseButton myValueField;
    private final Validator myValidator;

    public interface Validator {
        boolean checkName(String name);

        boolean isOK(String name, String value);
    }

    public PathMacroEditor(String title, String macroName, String value, Validator validator) {
        super(true);
        setTitle(title);
        myValidator = validator;
        myNameField.setText(macroName);
        DocumentListener documentListener = new DocumentAdapter() {
            @Override
            public void textChanged(DocumentEvent event) {
                updateControls();
            }
        };
        myNameField.getDocument().addDocumentListener(documentListener);
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
                    final int len = text.length();
                    if (len > 0 && text.charAt(len - 1) == File.separatorChar) {
                        text = text.substring(0, len - 1);
                    }
                    component.setText(text);
                }
            }
        );
        myValueField.getTextField().getDocument().addDocumentListener(documentListener);

        init();
        updateControls();
    }

    public void setMacroNameEditable(boolean isEditable) {
        myNameField.setEditable(isEditable);
    }

    private void updateControls() {
        final boolean isNameOK = myValidator.checkName(myNameField.getText());
        getOKAction().setEnabled(isNameOK);
        if (isNameOK) {
            final String text = myValueField.getText().trim();
            getOKAction().setEnabled(text.length() > 0 && !"/".equals(text.trim()));
        }
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myNameField;
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
        return myNameField.getText().trim();
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
