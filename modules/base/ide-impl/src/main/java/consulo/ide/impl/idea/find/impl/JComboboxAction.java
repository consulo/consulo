// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.find.impl;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindSettings;
import consulo.find.localize.FindLocalize;
import consulo.localization.LocalizedValue;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awt.event.DocumentAdapter;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemListener;
import java.util.function.Consumer;

/**
 * Action providing a combobox for file mask/filter selection in the SE text search toolbar.
 */
class JComboboxAction extends AnAction implements CustomComponentAction {
    private static final LocalizeValue EMPTY_TEXT = FindLocalize.seTextHeaderActionAllFiletypes();

    private final Project myProject;
    private final Disposable myDisposable;
    private final Consumer<@Nullable String> myOnChanged;
    private @Nullable String myLatestMask;

    final Runnable saveMask;

    JComboboxAction(Project project, Disposable disposable, Consumer<@Nullable String> onChanged) {
        myProject = project;
        myDisposable = disposable;
        myOnChanged = onChanged;
        myLatestMask = FindSettings.getInstance().getFileMask();
        saveMask = () -> FindSettings.getInstance().setFileMask(myLatestMask);
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation, String place) {
        return new ComboboxActionComponent(myProject, myDisposable, mask -> {
            myLatestMask = mask;
            myOnChanged.accept(mask);
        });
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        // no-op, combobox handles interaction
    }

    static class ComboboxActionComponent extends ComboBox<String> implements Disposable {
        private final FindModel myFindModel;
        private final @Nullable JTextField myTextField;
        private final ItemListener myItemListener;
        private final FindModel.FindModelObserver myFindModelObserver;
        private final DocumentAdapter myDocumentListener;
        private final FocusAdapter myFocusListener;

        ComboboxActionComponent(Project project, Disposable parentDisposable, Consumer<@Nullable String> onChanged) {
            super(FindSettings.getInstance().getRecentFileMasks());
            Disposer.register(parentDisposable, this);

            myFindModel = FindManager.getInstance(project).getFindInProjectModel();
            setEditor(new BasicComboBoxEditor());
            setMaximumRowCount(12);
            setPrototypeDisplayValue(EMPTY_TEXT.get());
            setOpaque(false);
            setEditable(true);

            insertItemAt(EMPTY_TEXT.get(), 0);

            String initialMask = FindSettings.getInstance().getFileMask();
            setSelectedItem(initialMask != null ? initialMask : EMPTY_TEXT);
            myFindModel.setFileFilter(initialMask);

            myTextField = getEditor().getEditorComponent() instanceof JTextField tf ? tf : null;

            Runnable rebuild = () -> {
                String normalizedText = getNormalizedText();
                myFindModel.setFileFilter(normalizedText);
                onChanged.accept(normalizedText);
            };

            myItemListener = e -> rebuild.run();
            addItemListener(myItemListener);

            myFindModelObserver = findModel -> {
                SwingUtilities.invokeLater(() -> {
                    String filter = findModel.getFileFilter();
                    setSelectedItem(filter != null ? filter : EMPTY_TEXT);
                });
            };
            myFindModel.addObserver(myFindModelObserver);

            myDocumentListener = new DocumentAdapter() {
                @Override
                protected void textChanged(DocumentEvent e) {
                    rebuild.run();
                }
            };

            myFocusListener = new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (getSelectedIndex() == 0) {
                        getEditor().setItem("");
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (myTextField != null && myTextField.getText().isEmpty()) {
                        myTextField.setText(EMPTY_TEXT.get());
                        setSelectedIndex(0);
                    }
                }
            };

            if (myTextField != null) {
                myTextField.addFocusListener(myFocusListener);
                myTextField.getDocument().addDocumentListener(myDocumentListener);
            }
        }

        @Override
        public void dispose() {
            removeItemListener(myItemListener);
            myFindModel.removeObserver(myFindModelObserver);
            if (myTextField != null) {
                myTextField.getDocument().removeDocumentListener(myDocumentListener);
                myTextField.removeFocusListener(myFocusListener);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(
                JBUI.scale(125),
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE.height() + getInsets().top + getInsets().bottom
            );
        }

        private @Nullable String getNormalizedText() {
            if (myTextField == null) return null;
            String text = myTextField.getText();
            return (EMPTY_TEXT.equals(text) || text.isBlank()) ? null : text;
        }
    }
}
