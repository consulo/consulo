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
package consulo.versionControlSystem.versionBrowser.ui.awt;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.versionBrowser.ChangeBrowserSettings;
import consulo.versionControlSystem.versionBrowser.ChangesBrowserSettingsEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class StandardVersionFilterComponent<T extends ChangeBrowserSettings> implements ChangesBrowserSettingsEditor<T> {
    private JPanel myPanel;

    protected JPanel getDatePanel() {
        return myDateFilterComponent.getPanel();
    }

    protected Component getStandardPanel() {
        return myPanel;
    }

    private JTextField myNumBefore;
    private JCheckBox myUseNumBeforeFilter;
    private JCheckBox myUseNumAfterFilter;
    private JTextField myNumAfter;
    private DateFilterComponent myDateFilterComponent;
    private JPanel myVersionNumberPanel;

    private T mySettings;

    public StandardVersionFilterComponent(boolean showDateFilter) {
        $$$setupUI$$$();

        myDateFilterComponent.getPanel().setVisible(showDateFilter);
    }

    protected void init(final T settings) {
        myVersionNumberPanel.setBorder(IdeBorderFactory.createTitledBorder(getChangeNumberTitle(), true));
        installCheckBoxesListeners();
        initValues(settings);
        updateAllEnabled(null);
    }

    protected void disableVersionNumbers() {
        myNumAfter.setVisible(false);
        myNumBefore.setVisible(false);
        myUseNumBeforeFilter.setVisible(false);
        myUseNumAfterFilter.setVisible(false);
    }

    protected String getChangeNumberTitle() {
        return VcsBundle.message("border.changes.filter.change.number.filter");
    }

    private void installCheckBoxesListeners() {
        final ActionListener filterListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAllEnabled(e);
            }
        };


        installCheckBoxListener(filterListener);
    }

    public static void updatePair(JCheckBox checkBox, JComponent textField, ActionEvent e) {
        textField.setEnabled(checkBox.isSelected());
        if (e != null && e.getSource() instanceof JCheckBox && ((JCheckBox) e.getSource()).isSelected()) {
            final Object source = e.getSource();
            if (source == checkBox && checkBox.isSelected()) {
                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
                    IdeFocusManager.getGlobalInstance().requestFocus(textField, true);
                });
            }
        }

    }

    protected void updateAllEnabled(final ActionEvent e) {
        updatePair(myUseNumBeforeFilter, myNumBefore, e);
        updatePair(myUseNumAfterFilter, myNumAfter, e);
    }

    protected void initValues(T settings) {
        myUseNumBeforeFilter.setSelected(settings.USE_CHANGE_BEFORE_FILTER);
        myUseNumAfterFilter.setSelected(settings.USE_CHANGE_AFTER_FILTER);

        myDateFilterComponent.initValues(settings);
        myNumBefore.setText(settings.CHANGE_BEFORE);
        myNumAfter.setText(settings.CHANGE_AFTER);
    }

    public void saveValues(T settings) {
        myDateFilterComponent.saveValues(settings);
        settings.USE_CHANGE_BEFORE_FILTER = myUseNumBeforeFilter.isSelected();
        settings.USE_CHANGE_AFTER_FILTER = myUseNumAfterFilter.isSelected();

        settings.CHANGE_BEFORE = myNumBefore.getText();
        settings.CHANGE_AFTER = myNumAfter.getText();
    }

    protected void installCheckBoxListener(final ActionListener filterListener) {
        myUseNumBeforeFilter.addActionListener(filterListener);
        myUseNumAfterFilter.addActionListener(filterListener);
    }

    @Override
    public T getSettings() {
        saveValues(mySettings);
        return mySettings;
    }

    @Override
    public void setSettings(T settings) {
        mySettings = settings;
        initValues(settings);
        updateAllEnabled(null);
    }

    @Override
    public String validateInput() {
        if (myUseNumAfterFilter.isSelected()) {
            try {
                Long.parseLong(myNumAfter.getText());
            }
            catch (NumberFormatException ex) {
                return getChangeNumberTitle() + " From must be a valid number";
            }
        }
        if (myUseNumBeforeFilter.isSelected()) {
            try {
                Long.parseLong(myNumBefore.getText());
            }
            catch (NumberFormatException ex) {
                return getChangeNumberTitle() + " To must be a valid number";
            }
        }
        return myDateFilterComponent.validateInput();
    }

    @Override
    public void updateEnabledControls() {
        updateAllEnabled(null);
    }

    @Override
    public String getDimensionServiceKey() {
        return getClass().getName();
    }

    /**
     * Method generated by Consulo GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        myPanel = new JPanel();
        myPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        myVersionNumberPanel = new JPanel();
        myVersionNumberPanel.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        myPanel.add(myVersionNumberPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myUseNumAfterFilter = new JCheckBox();
        this.$$$loadButtonText$$$(myUseNumAfterFilter, VcsBundle.message("checkbox.show.changes.after.num"));
        myVersionNumberPanel.add(myUseNumAfterFilter, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myNumAfter = new JTextField();
        myVersionNumberPanel.add(myNumAfter, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        myUseNumBeforeFilter = new JCheckBox();
        this.$$$loadButtonText$$$(myUseNumBeforeFilter, VcsBundle.message("checkbox.show.changes.before.num"));
        myVersionNumberPanel.add(myUseNumBeforeFilter, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myNumBefore = new JTextField();
        myVersionNumberPanel.add(myNumBefore, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        myDateFilterComponent = new DateFilterComponent();
        myPanel.add(myDateFilterComponent.$$$getRootComponent$$$(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        return myPanel;
    }
}

  
