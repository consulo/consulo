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

import consulo.disposer.Disposer;
import consulo.execution.debug.XBreakpointManager;
import consulo.execution.debug.XDebuggerHistoryManager;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.breakpoint.XBreakpointType;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.breakpoint.ui.XBreakpointCustomPropertiesPanel;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.execution.debug.impl.internal.ui.UnifiedXDebuggerExpressionComboBox;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class XLightBreakpointPropertiesPanel implements XSuspendPolicyPanel.Delegate {
    public interface Delegate {
        void showMoreOptions();
    }

    private final VerticalLayout myMainPanel = VerticalLayout.create();

    private final Label myBreakpointNameLabel = Label.create();
    private final CheckBox myEnabledCheckbox = CheckBox.create(XDebuggerLocalize.xbreakpointsEnabledLabel());

    private final XSuspendPolicyPanel mySuspendPolicyPanel = new XSuspendPolicyPanel();
    private final XBreakpointActionsPanel myActionsPanel = new XBreakpointActionsPanel();
    private final XMasterBreakpointPanel myMasterBreakpointPanel = new XMasterBreakpointPanel();

    private final List<XBreakpointCustomPropertiesPanel<?>> myCustomPanels = new ArrayList<>();
    private final List<XBreakpointPropertiesSubPanel> mySubPanels = new ArrayList<>();

    private @Nullable CheckBox myConditionEnabledCheckbox;
    private @Nullable Label myConditionLabel;
    private @Nullable UnifiedXDebuggerExpressionComboBox myConditionComboBox;

    private final XBreakpointBase myBreakpoint;
    private final boolean myShowAllOptions;

    private boolean myShowMoreOptions;

    private @Nullable Delegate myDelegate;

    @RequiredUIAccess
    public XLightBreakpointPropertiesPanel(Project project, XBreakpointManager breakpointManager, XBreakpointBase breakpoint, boolean showAllOptions) {
        myBreakpoint = breakpoint;
        myShowAllOptions = showAllOptions;
        XBreakpointType breakpointType = breakpoint.getType();

        myMainPanel.add(myBreakpointNameLabel);
        myMainPanel.add(myEnabledCheckbox);

        XBreakpointCustomPropertiesPanel customTopPropertiesPanel = breakpointType.createCustomTopPropertiesPanel(project);
        if (customTopPropertiesPanel != null) {
            myMainPanel.add(toUIComponent(customTopPropertiesPanel));
            myCustomPanels.add(customTopPropertiesPanel);
        }

        if (breakpointType.getVisibleStandardPanels().contains(XBreakpointType.StandardPanels.SUSPEND_POLICY)) {
            mySuspendPolicyPanel.init(project, breakpointManager, breakpoint);
            mySuspendPolicyPanel.setDelegate(this);
            mySubPanels.add(mySuspendPolicyPanel);
            myMainPanel.add(mySuspendPolicyPanel.getComponent());
        }

        XDebuggerEditorsProvider debuggerEditorsProvider = breakpointType.getEditorsProvider(breakpoint, project);

        if (debuggerEditorsProvider != null) {
            CheckBox conditionEnabledCheckbox = CheckBox.create(XDebuggerLocalize.xbreakpointsConditionCheckbox());
            myConditionEnabledCheckbox = conditionEnabledCheckbox;

            Label conditionLabel = Label.create(XDebuggerLocalize.xbreakpointsConditionCheckbox());
            myConditionLabel = conditionLabel;

            UnifiedXDebuggerExpressionComboBox conditionComboBox = new UnifiedXDebuggerExpressionComboBox(
                project,
                debuggerEditorsProvider,
                XDebuggerHistoryManager.BREAKPOINT_CONDITION_HISTORY_ID,
                myBreakpoint.getSourcePosition(),
                true
            );
            myConditionComboBox = conditionComboBox;

            conditionEnabledCheckbox.addValueListener(event -> {
                onCheckboxChanged();
                if (conditionEnabledCheckbox.getValueOrError()) {
                    conditionComboBox.requestFocusInEditor();
                }
            });

            myMainPanel.add(DockLayout.create()
                .left(HorizontalLayout.create().add(conditionEnabledCheckbox).add(conditionLabel))
                .center(conditionComboBox.getUIComponent()));
        }

        XBreakpointCustomPropertiesPanel customConditionPanel = breakpointType.createCustomConditionsPanel();
        if (customConditionPanel != null) {
            myMainPanel.add(toUIComponent(customConditionPanel));
            myCustomPanels.add(customConditionPanel);
        }

        if (breakpointType.getVisibleStandardPanels().contains(XBreakpointType.StandardPanels.ACTIONS)) {
            myActionsPanel.init(project, breakpointManager, breakpoint, debuggerEditorsProvider);
            mySubPanels.add(myActionsPanel);
        }

        XBreakpointCustomPropertiesPanel customRightConditionPanel = breakpointType.createCustomRightPropertiesPanel(project);
        boolean showRight = customRightConditionPanel != null && (showAllOptions || customRightConditionPanel.isVisibleOnPopup(breakpoint));
        if (mySubPanels.contains(myActionsPanel) || showRight) {
            HorizontalLayout actionsRow = HorizontalLayout.create();
            if (mySubPanels.contains(myActionsPanel)) {
                actionsRow.add(myActionsPanel.getComponent());
            }
            if (showRight) {
                actionsRow.add(toUIComponent(customRightConditionPanel));
                myCustomPanels.add(customRightConditionPanel);
            }
            myMainPanel.add(actionsRow);
        }

        if (breakpointType.getVisibleStandardPanels().contains(XBreakpointType.StandardPanels.DEPENDENCY)) {
            myMasterBreakpointPanel.init(project, breakpointManager, breakpoint);
            mySubPanels.add(myMasterBreakpointPanel);
            myMainPanel.add(myMasterBreakpointPanel.getComponent());
        }

        myShowMoreOptions = false;
        for (XBreakpointPropertiesSubPanel panel : mySubPanels) {
            if (panel.lightVariant(showAllOptions)) {
                myShowMoreOptions = true;
            }
        }

        XBreakpointCustomPropertiesPanel customPropertiesPanel = breakpointType.createCustomPropertiesPanel(project);
        if (customPropertiesPanel != null) {
            myMainPanel.add(toUIComponent(customPropertiesPanel));
            myCustomPanels.add(customPropertiesPanel);
        }

        myEnabledCheckbox.addValueListener(event -> saveProperties());
    }

    @RequiredUIAccess
    private static Component toUIComponent(XBreakpointCustomPropertiesPanel<?> panel) {
        try {
            return panel.getUIComponent();
        }
        catch (AbstractMethodError e) {
            return TargetAWT.wrap(panel.getComponent());
        }
    }

    public boolean showMoreOptions() {
        return myShowMoreOptions;
    }

    @Override
    public void showMoreOptionsIfNeeded() {
        Delegate delegate = myDelegate;
        if (myShowMoreOptions && delegate != null) {
            delegate.showMoreOptions();
        }
    }

    public @Nullable Delegate getDelegate() {
        return myDelegate;
    }

    public void setDelegate(@Nullable Delegate delegate) {
        myDelegate = delegate;
    }

    @RequiredUIAccess
    public void requestDefaultFocus() {
        UnifiedXDebuggerExpressionComboBox conditionComboBox = myConditionComboBox;
        if (conditionComboBox != null && conditionComboBox.getComboBox().isEnabled()) {
            conditionComboBox.requestFocusInEditor();
        }
        else if (mySubPanels.contains(myActionsPanel)) {
            myActionsPanel.focusDefaultComponent();
        }
    }

    @RequiredUIAccess
    private void onCheckboxChanged() {
        UnifiedXDebuggerExpressionComboBox conditionComboBox = myConditionComboBox;
        CheckBox conditionEnabledCheckbox = myConditionEnabledCheckbox;
        if (conditionComboBox != null && conditionEnabledCheckbox != null) {
            conditionComboBox.setEnabled(conditionEnabledCheckbox.getValueOrError());
        }
    }

    @RequiredUIAccess
    public void saveProperties() {
        for (XBreakpointPropertiesSubPanel panel : mySubPanels) {
            panel.saveProperties();
        }

        UnifiedXDebuggerExpressionComboBox conditionComboBox = myConditionComboBox;
        CheckBox conditionEnabledCheckbox = myConditionEnabledCheckbox;
        if (conditionComboBox != null && conditionEnabledCheckbox != null) {
            XExpression expression = conditionComboBox.getExpression();
            XExpression condition = !XDebuggerUtil.getInstance().isEmptyExpression(expression) ? expression : null;
            myBreakpoint.setConditionEnabled(condition == null || conditionEnabledCheckbox.getValueOrError());
            myBreakpoint.setConditionExpression(condition);
            conditionComboBox.saveTextInHistory();
        }

        for (XBreakpointCustomPropertiesPanel customPanel : myCustomPanels) {
            customPanel.saveTo(myBreakpoint);
        }
        myBreakpoint.setEnabled(myEnabledCheckbox.getValueOrError());
    }

    @RequiredUIAccess
    public void loadProperties() {
        for (XBreakpointPropertiesSubPanel panel : mySubPanels) {
            panel.loadProperties();
        }

        UnifiedXDebuggerExpressionComboBox conditionComboBox = myConditionComboBox;
        CheckBox conditionEnabledCheckbox = myConditionEnabledCheckbox;
        Label conditionLabel = myConditionLabel;
        if (conditionComboBox != null && conditionEnabledCheckbox != null && conditionLabel != null) {
            XExpression condition = myBreakpoint.getConditionExpressionInt();
            conditionComboBox.setExpression(condition);
            boolean hideCheckbox = !myShowAllOptions && condition == null;
            conditionEnabledCheckbox.setValue(hideCheckbox || (myBreakpoint.isConditionEnabled() && condition != null), false);
            conditionEnabledCheckbox.setVisible(!hideCheckbox);
            conditionLabel.setVisible(hideCheckbox);

            onCheckboxChanged();
        }

        for (XBreakpointCustomPropertiesPanel customPanel : myCustomPanels) {
            customPanel.loadFrom(myBreakpoint);
        }
        myEnabledCheckbox.setValue(myBreakpoint.isEnabled(), false);
        myBreakpointNameLabel.setText(LocalizeValue.of(XBreakpointUtil.getShortText(myBreakpoint)));
    }

    public Component getMainPanel() {
        return myMainPanel;
    }

    public void dispose() {
        myActionsPanel.dispose();
        for (XBreakpointCustomPropertiesPanel panel : myCustomPanels) {
            Disposer.dispose(panel);
        }
    }
}
