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

import consulo.execution.debug.XBreakpointManager;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.impl.internal.ui.UnifiedXDebuggerExpressionComboBox;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author zajac
 * @since 2011-06-18
 */
public class XBreakpointActionsPanel extends XBreakpointPropertiesSubPanel {
    public static final String LOG_EXPRESSION_HISTORY_ID = "breakpointLogExpression";

    private final VerticalLayout myMainPanel = VerticalLayout.create();
    private final LabeledLayout myContentPane = LabeledLayout.create(XDebuggerLocalize.xbreakpointsPropertiesActionsGroupTitle(), myMainPanel);

    private CheckBox myLogMessageCheckBox;
    private @Nullable CheckBox myLogExpressionCheckBox;
    private CheckBox myTemporaryCheckBox;
    private @Nullable UnifiedXDebuggerExpressionComboBox myLogExpressionComboBox;

    @RequiredUIAccess
    public void init(
        Project project,
        XBreakpointManager breakpointManager,
        XBreakpointBase breakpoint,
        @Nullable XDebuggerEditorsProvider debuggerEditorsProvider
    ) {
        init(project, breakpointManager, breakpoint);

        myLogMessageCheckBox = CheckBox.create(XDebuggerLocalize.xbreakpointsLogMessageCheckbox());
        myMainPanel.add(myLogMessageCheckBox);

        myTemporaryCheckBox = CheckBox.create(XDebuggerLocalize.xbreakpointsRemoveOnceHitCheckbox());

        if (debuggerEditorsProvider != null) {
            CheckBox logExpressionCheckBox = CheckBox.create(XDebuggerLocalize.xbreakpointsLogExpressionCheckbox());
            myLogExpressionCheckBox = logExpressionCheckBox;

            UnifiedXDebuggerExpressionComboBox comboBox = new UnifiedXDebuggerExpressionComboBox(
                project,
                debuggerEditorsProvider,
                LOG_EXPRESSION_HISTORY_ID,
                myBreakpoint.getSourcePosition(),
                true
            );
            myLogExpressionComboBox = comboBox;
            comboBox.setEnabled(false);

            myMainPanel.add(logExpressionCheckBox);
            myMainPanel.add(comboBox.getUIComponent());

            myTemporaryCheckBox.setVisible(breakpoint instanceof XLineBreakpoint);
            logExpressionCheckBox.addValueListener(event -> {
                onCheckboxChanged();
                if (logExpressionCheckBox.getValueOrError()) {
                    comboBox.requestFocusInEditor();
                }
            });
        }

        myMainPanel.add(myTemporaryCheckBox);
    }

    @Override
    public Component getComponent() {
        return myContentPane;
    }

    @RequiredUIAccess
    @Override
    public boolean lightVariant(boolean showAllOptions) {
        if (!showAllOptions && !myBreakpoint.isLogMessage() && myBreakpoint.getLogExpression() == null
            && (!(myBreakpoint instanceof XLineBreakpoint lineBreakpoint) || !lineBreakpoint.isTemporary())) {
            myContentPane.setVisible(false);
            return true;
        }
        return false;
    }

    @RequiredUIAccess
    private void onCheckboxChanged() {
        UnifiedXDebuggerExpressionComboBox comboBox = myLogExpressionComboBox;
        CheckBox checkBox = myLogExpressionCheckBox;
        if (comboBox != null && checkBox != null) {
            comboBox.setEnabled(checkBox.getValueOrError());
        }
    }

    @RequiredUIAccess
    @Override
    void loadProperties() {
        myLogMessageCheckBox.setValue(myBreakpoint.isLogMessage(), false);

        if (myBreakpoint instanceof XLineBreakpoint lineBreakpoint) {
            myTemporaryCheckBox.setValue(lineBreakpoint.isTemporary(), false);
        }

        UnifiedXDebuggerExpressionComboBox comboBox = myLogExpressionComboBox;
        CheckBox checkBox = myLogExpressionCheckBox;
        if (comboBox != null && checkBox != null) {
            XExpression logExpression = myBreakpoint.getLogExpressionObjectInt();
            comboBox.setExpression(logExpression);
            checkBox.setValue(myBreakpoint.isLogExpressionEnabled() && logExpression != null, false);
        }
        onCheckboxChanged();
    }

    @RequiredUIAccess
    @Override
    void saveProperties() {
        myBreakpoint.setLogMessage(myLogMessageCheckBox.getValueOrError());

        if (myBreakpoint instanceof XLineBreakpoint lineBreakpoint) {
            lineBreakpoint.setTemporary(myTemporaryCheckBox.getValueOrError());
        }

        UnifiedXDebuggerExpressionComboBox comboBox = myLogExpressionComboBox;
        CheckBox checkBox = myLogExpressionCheckBox;
        if (comboBox != null && checkBox != null) {
            XExpression expression = comboBox.getExpression();
            XExpression logExpression = !XDebuggerUtil.getInstance().isEmptyExpression(expression) ? expression : null;
            myBreakpoint.setLogExpressionEnabled(logExpression == null || checkBox.getValueOrError());
            myBreakpoint.setLogExpressionObject(logExpression);
            comboBox.saveTextInHistory();
        }
    }

    @RequiredUIAccess
    boolean focusDefaultComponent() {
        UnifiedXDebuggerExpressionComboBox comboBox = myLogExpressionComboBox;
        if (comboBox != null && comboBox.getComboBox().isEnabled()) {
            comboBox.requestFocusInEditor();
            return true;
        }
        return false;
    }

    public void dispose() {
    }

    public void hide() {
        myContentPane.setVisible(false);
    }
}
