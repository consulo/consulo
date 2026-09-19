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
import consulo.ui.CheckBox;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.versionBrowser.ChangeBrowserSettings;
import consulo.versionControlSystem.versionBrowser.ChangesBrowserSettingsEditor;

import javax.swing.*;
import java.awt.*;

public abstract class StandardVersionFilterComponent<T extends ChangeBrowserSettings> implements ChangesBrowserSettingsEditor<T> {
    private JPanel myPanel;

    protected JPanel getDatePanel() {
        return myDateFilterComponent.getPanel();
    }

    protected Component getStandardPanel() {
        return myPanel;
    }

    private JTextField myNumBefore;
    private CheckBox myUseNumBeforeFilter;
    private CheckBox myUseNumAfterFilter;
    private JTextField myNumAfter;
    private DateFilterComponent myDateFilterComponent;
    private JPanel myVersionNumberPanel;

    private T mySettings;

    @RequiredUIAccess
    public StandardVersionFilterComponent(boolean showDateFilter) {
        myPanel = new JPanel();
        myPanel.setLayout(new GridLayoutManager(2, 1, JBUI.emptyInsets(), -1, -1));
        myVersionNumberPanel = new JPanel();
        myVersionNumberPanel.setLayout(new GridLayoutManager(1, 4, JBUI.emptyInsets(), -1, -1));
        myPanel.add(myVersionNumberPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        myUseNumAfterFilter = CheckBox.create(VcsLocalize.checkboxShowChangesAfterNum());
        myVersionNumberPanel.add(TargetAWT.to(myUseNumAfterFilter), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myNumAfter = new JTextField();
        myVersionNumberPanel.add(myNumAfter, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        myUseNumBeforeFilter = CheckBox.create(VcsLocalize.checkboxShowChangesBeforeNum());
        myVersionNumberPanel.add(TargetAWT.to(myUseNumBeforeFilter), new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myNumBefore = new JTextField();
        myVersionNumberPanel.add(myNumBefore, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        myDateFilterComponent = new DateFilterComponent();
        myPanel.add(myDateFilterComponent.$$$getRootComponent$$$(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        myDateFilterComponent.getPanel().setVisible(showDateFilter);
    }

    @RequiredUIAccess
    protected void init(T settings) {
        myVersionNumberPanel.setBorder(IdeBorderFactory.createTitledBorder(getChangeNumberTitle(), true));
        installCheckBoxesListeners();
        initValues(settings);
        updateAllEnabled(null);
    }

    @RequiredUIAccess
    protected void disableVersionNumbers() {
        myNumAfter.setVisible(false);
        myNumBefore.setVisible(false);
        myUseNumBeforeFilter.setVisible(false);
        myUseNumAfterFilter.setVisible(false);
    }

    protected String getChangeNumberTitle() {
        return VcsLocalize.borderChangesFilterChangeNumberFilter().get();
    }

    private void installCheckBoxesListeners() {
        installCheckBoxListener(this::updateAllEnabled);
    }

    public static void updatePair(CheckBox checkBox, JComponent textField, ValueComponentEvent<Boolean> e) {
        textField.setEnabled(checkBox.getValue());
        if (e != null && e.getValue()) {
            Object source = e.getComponent();
            if (source == checkBox && checkBox.getValue()) {
                IdeFocusManager.getGlobalInstance()
                    .doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(textField, true));
            }
        }
    }

    protected void updateAllEnabled(ValueComponentEvent<Boolean> e) {
        updatePair(myUseNumBeforeFilter, myNumBefore, e);
        updatePair(myUseNumAfterFilter, myNumAfter, e);
    }

    @RequiredUIAccess
    protected void initValues(T settings) {
        myUseNumBeforeFilter.setValue(settings.USE_CHANGE_BEFORE_FILTER, false);
        myUseNumAfterFilter.setValue(settings.USE_CHANGE_AFTER_FILTER, false);

        myDateFilterComponent.initValues(settings);
        myNumBefore.setText(settings.CHANGE_BEFORE);
        myNumAfter.setText(settings.CHANGE_AFTER);
    }

    public void saveValues(T settings) {
        myDateFilterComponent.saveValues(settings);
        settings.USE_CHANGE_BEFORE_FILTER = myUseNumBeforeFilter.getValue();
        settings.USE_CHANGE_AFTER_FILTER = myUseNumAfterFilter.getValue();

        settings.CHANGE_BEFORE = myNumBefore.getText();
        settings.CHANGE_AFTER = myNumAfter.getText();
    }

    protected void installCheckBoxListener(ComponentEventListener<ValueComponent<Boolean>, ValueComponentEvent<Boolean>> filterListener) {
        myUseNumBeforeFilter.addValueListener(filterListener);
        myUseNumAfterFilter.addValueListener(filterListener);
    }

    @Override
    public T getSettings() {
        saveValues(mySettings);
        return mySettings;
    }

    @Override
    @RequiredUIAccess
    public void setSettings(T settings) {
        mySettings = settings;
        initValues(settings);
        updateAllEnabled(null);
    }

    @Override
    public String validateInput() {
        if (myUseNumAfterFilter.getValue()) {
            try {
                Long.parseLong(myNumAfter.getText());
            }
            catch (NumberFormatException ex) {
                return getChangeNumberTitle() + " From must be a valid number";
            }
        }
        if (myUseNumBeforeFilter.getValue()) {
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

    public JComponent $$$getRootComponent$$$() {
        return myPanel;
    }
}
