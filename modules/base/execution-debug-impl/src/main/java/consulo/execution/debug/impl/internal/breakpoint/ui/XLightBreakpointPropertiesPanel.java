/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.breakpoint.ui;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.util.popup.DetailView;
import consulo.disposer.Disposer;
import consulo.execution.debug.XBreakpointManager;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.XDebuggerHistoryManager;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.breakpoint.XBreakpointType;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.breakpoint.ui.XBreakpointCustomPropertiesPanel;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.execution.debug.impl.internal.ui.DebuggerUIImplUtil;
import consulo.execution.debug.impl.internal.ui.XDebuggerExpressionComboBox;
import consulo.project.Project;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

public class XLightBreakpointPropertiesPanel implements XSuspendPolicyPanel.Delegate {
    @SuppressWarnings("UnusedDeclaration")
    public boolean showMoreOptions() {
        return myShowMoreOptions;
    }

    private boolean myShowMoreOptions;

    @Override
    public void showMoreOptionsIfNeeded() {
        if (myShowMoreOptions) {
            if (myDelegate != null) {
                myDelegate.showMoreOptions();
            }
        }
    }

    public interface Delegate {
        void showMoreOptions();
    }

    private JPanel myConditionExpressionPanel;
    private JPanel myConditionPanel;
    private JPanel myMainPanel;

    public Delegate getDelegate() {
        return myDelegate;
    }

    public void setDelegate(Delegate delegate) {
        myDelegate = delegate;
    }

    private Delegate myDelegate;

    private XSuspendPolicyPanel mySuspendPolicyPanel;
    private XBreakpointActionsPanel myActionsPanel;
    private XMasterBreakpointPanel myMasterBreakpointPanel;
    private JPanel myCustomPropertiesPanelWrapper;
    private JPanel myCustomConditionsPanelWrapper;
    private JCheckBox myEnabledCheckbox;
    private JPanel myCustomRightPropertiesPanelWrapper;
    private JBCheckBox myConditionEnabledCheckbox;
    private JPanel myCustomTopPropertiesPanelWrapper;
    private JPanel myConditionEnabledPanel;
    private JBLabel myBreakpointNameLabel;
    private final List<XBreakpointCustomPropertiesPanel<?>> myCustomPanels;

    private final List<XBreakpointPropertiesSubPanel> mySubPanels = new ArrayList<>();

    private XDebuggerExpressionComboBox myConditionComboBox;

    private final XBreakpointBase myBreakpoint;

    private final boolean myShowAllOptions;

    public void setDetailView(DetailView detailView) {
        myMasterBreakpointPanel.setDetailView(detailView);
    }

    public XLightBreakpointPropertiesPanel(Project project, XBreakpointManager breakpointManager, XBreakpointBase breakpoint, boolean showAllOptions) {
        myBreakpoint = breakpoint;
        myShowAllOptions = showAllOptions;
        XBreakpointType breakpointType = breakpoint.getType();

        if (breakpointType.getVisibleStandardPanels().contains(XBreakpointType.StandardPanels.SUSPEND_POLICY)) {
            mySuspendPolicyPanel.init(project, breakpointManager, breakpoint);
            mySuspendPolicyPanel.setDelegate(this);
            mySubPanels.add(mySuspendPolicyPanel);
        }
        else {
            mySuspendPolicyPanel.hide();
        }

        if (breakpointType.getVisibleStandardPanels().contains(XBreakpointType.StandardPanels.DEPENDENCY)) {
            myMasterBreakpointPanel.init(project, breakpointManager, breakpoint);
            mySubPanels.add(myMasterBreakpointPanel);
        }
        else {
            myMasterBreakpointPanel.hide();
        }

        XDebuggerEditorsProvider debuggerEditorsProvider = breakpointType.getEditorsProvider(breakpoint, project);

        if (breakpointType.getVisibleStandardPanels().contains(XBreakpointType.StandardPanels.ACTIONS)) {
            myActionsPanel.init(project, breakpointManager, breakpoint, debuggerEditorsProvider);
            mySubPanels.add(myActionsPanel);
        }
        else {
            myActionsPanel.hide();
        }

        myCustomPanels = new ArrayList<>();
        if (debuggerEditorsProvider != null) {
            myConditionEnabledCheckbox = new JBCheckBox(XDebuggerBundle.message("xbreakpoints.condition.checkbox"));
            myConditionComboBox = new XDebuggerExpressionComboBox(project, debuggerEditorsProvider, XDebuggerHistoryManager.BREAKPOINT_CONDITION_HISTORY_ID, myBreakpoint.getSourcePosition(), true);
            JComponent conditionComponent = myConditionComboBox.getComponent();
            conditionComponent.setBorder(JBUI.Borders.emptyRight(3));
            myConditionExpressionPanel.add(conditionComponent, BorderLayout.CENTER);
            myConditionEnabledCheckbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onCheckboxChanged();
                }
            });
            DebuggerUIImplUtil.focusEditorOnCheck(myConditionEnabledCheckbox, myConditionComboBox.getEditorComponent());
        }
        else {
            myConditionPanel.setVisible(false);
        }

        myShowMoreOptions = false;
        for (XBreakpointPropertiesSubPanel panel : mySubPanels) {
            if (panel.lightVariant(showAllOptions)) {
                myShowMoreOptions = true;
            }
        }

        XBreakpointCustomPropertiesPanel customPropertiesPanel = breakpointType.createCustomPropertiesPanel(project);
        if (customPropertiesPanel != null) {
            myCustomPropertiesPanelWrapper.add(customPropertiesPanel.getComponent(), BorderLayout.CENTER);
            myCustomPanels.add(customPropertiesPanel);
        }
        else {
            myCustomPropertiesPanelWrapper.getParent().remove(myCustomPropertiesPanelWrapper);
        }

        XBreakpointCustomPropertiesPanel customConditionPanel = breakpointType.createCustomConditionsPanel();
        if (customConditionPanel != null) {
            myCustomConditionsPanelWrapper.add(customConditionPanel.getComponent(), BorderLayout.CENTER);
            myCustomPanels.add(customConditionPanel);
        }
        else {
            myCustomConditionsPanelWrapper.getParent().remove(myCustomConditionsPanelWrapper);
        }

        XBreakpointCustomPropertiesPanel customRightConditionPanel = breakpointType.createCustomRightPropertiesPanel(project);
        if (customRightConditionPanel != null && (showAllOptions || customRightConditionPanel.isVisibleOnPopup(breakpoint))) {
            myCustomRightPropertiesPanelWrapper.add(customRightConditionPanel.getComponent(), BorderLayout.CENTER);
            myCustomPanels.add(customRightConditionPanel);
        }
        else {
            // see IDEA-125745
            myCustomRightPropertiesPanelWrapper.getParent().remove(myCustomRightPropertiesPanelWrapper);
        }

        XBreakpointCustomPropertiesPanel customTopPropertiesPanel = breakpointType.createCustomTopPropertiesPanel(project);
        if (customTopPropertiesPanel != null) {
            myCustomTopPropertiesPanelWrapper.add(customTopPropertiesPanel.getComponent(), BorderLayout.CENTER);
            myCustomPanels.add(customTopPropertiesPanel);
        }
        else {
            myCustomTopPropertiesPanelWrapper.getParent().remove(myCustomTopPropertiesPanelWrapper);
        }

        myMainPanel.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                JComponent compToFocus = null;
                if (myConditionComboBox != null && myConditionComboBox.getComboBox().isEnabled()) {
                    compToFocus = myConditionComboBox.getEditorComponent();
                }
                else if (breakpointType.getVisibleStandardPanels().contains(XBreakpointType.StandardPanels.ACTIONS)) {
                    compToFocus = myActionsPanel.getDefaultFocusComponent();
                }
                if (compToFocus != null) {
                    IdeFocusManager.findInstance().requestFocus(compToFocus, false);
                }
            }
        });

        myEnabledCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                myBreakpoint.setEnabled(myEnabledCheckbox.isSelected());
            }
        });
    }

    private void onCheckboxChanged() {
        if (myConditionComboBox != null) {
            myConditionComboBox.setEnabled(myConditionEnabledCheckbox.isSelected());
        }
    }

    public void saveProperties() {
        for (XBreakpointPropertiesSubPanel panel : mySubPanels) {
            panel.saveProperties();
        }

        if (myConditionComboBox != null) {
            XExpression expression = myConditionComboBox.getExpression();
            XExpression condition = !XDebuggerUtil.getInstance().isEmptyExpression(expression) ? expression : null;
            myBreakpoint.setConditionEnabled(condition == null || myConditionEnabledCheckbox.isSelected());
            myBreakpoint.setConditionExpression(condition);
            myConditionComboBox.saveTextInHistory();
        }

        for (XBreakpointCustomPropertiesPanel customPanel : myCustomPanels) {
            customPanel.saveTo(myBreakpoint);
        }
        myBreakpoint.setEnabled(myEnabledCheckbox.isSelected());
    }

    public void loadProperties() {
        for (XBreakpointPropertiesSubPanel panel : mySubPanels) {
            panel.loadProperties();
        }

        if (myConditionComboBox != null) {
            XExpression condition = myBreakpoint.getConditionExpressionInt();
            myConditionComboBox.setExpression(condition);
            boolean hideCheckbox = !myShowAllOptions && condition == null;
            myConditionEnabledCheckbox.setSelected(hideCheckbox || (myBreakpoint.isConditionEnabled() && condition != null));
            myConditionEnabledPanel.removeAll();
            if (hideCheckbox) {
                JBLabel label = new JBLabel(XDebuggerBundle.message("xbreakpoints.condition.checkbox"));
                label.setBorder(JBUI.Borders.empty(0, 4));
                label.setLabelFor(myConditionComboBox.getComboBox());
                myConditionEnabledPanel.add(label);
            }
            else {
                myConditionEnabledPanel.add(myConditionEnabledCheckbox);
            }

            onCheckboxChanged();
        }

        for (XBreakpointCustomPropertiesPanel customPanel : myCustomPanels) {
            customPanel.loadFrom(myBreakpoint);
        }
        myEnabledCheckbox.setSelected(myBreakpoint.isEnabled());
        myBreakpointNameLabel.setText(XBreakpointUtil.getShortText(myBreakpoint));
    }

    public JPanel getMainPanel() {
        return myMainPanel;
    }

    public void dispose() {
        myActionsPanel.dispose();
        for (XBreakpointCustomPropertiesPanel panel : myCustomPanels) {
            Disposer.dispose(panel);
        }
    }
}
