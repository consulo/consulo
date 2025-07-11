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
import consulo.execution.debug.impl.internal.ui.DebuggerUIImplUtil;
import consulo.execution.debug.impl.internal.ui.XDebuggerExpressionComboBox;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author zajac
 * @since 2011-06-18
 */
public class XBreakpointActionsPanel extends XBreakpointPropertiesSubPanel {
  public static final String LOG_EXPRESSION_HISTORY_ID = "breakpointLogExpression";

  private JCheckBox myLogMessageCheckBox;
  private JCheckBox myLogExpressionCheckBox;
  private JPanel myLogExpressionPanel;
  private JPanel myContentPane;
  private JPanel myMainPanel;
  private JCheckBox myTemporaryCheckBox;
  private JPanel myExpressionPanel;
  private XDebuggerExpressionComboBox myLogExpressionComboBox;

  public void init(Project project, XBreakpointManager breakpointManager, @Nonnull XBreakpointBase breakpoint, @Nullable XDebuggerEditorsProvider debuggerEditorsProvider) {
    init(project, breakpointManager, breakpoint);
    if (debuggerEditorsProvider != null) {
      ActionListener listener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          onCheckboxChanged();
        }
      };
      myLogExpressionComboBox = new XDebuggerExpressionComboBox(project, debuggerEditorsProvider, LOG_EXPRESSION_HISTORY_ID, myBreakpoint.getSourcePosition(), true);
      JComponent logExpressionComponent = myLogExpressionComboBox.getComponent();
      myLogExpressionPanel.add(logExpressionComponent, BorderLayout.CENTER);
      myLogExpressionComboBox.setEnabled(false);
      myTemporaryCheckBox.setVisible(breakpoint instanceof XLineBreakpoint);
      myLogExpressionCheckBox.addActionListener(listener);
      DebuggerUIImplUtil.focusEditorOnCheck(myLogExpressionCheckBox, myLogExpressionComboBox.getEditorComponent());
    }
    else {
      myExpressionPanel.getParent().remove(myExpressionPanel);
    }
  }

  @Override
  public boolean lightVariant(boolean showAllOptions) {
    if (!showAllOptions && !myBreakpoint.isLogMessage() && myBreakpoint.getLogExpression() == null &&
        (!(myBreakpoint instanceof XLineBreakpoint) || !((XLineBreakpoint)myBreakpoint).isTemporary()) ) {
      myMainPanel.setVisible(false);
      return true;
    } else {
      myMainPanel.setBorder(null);
      return false;
    }
  }

  private void onCheckboxChanged() {
    if (myLogExpressionComboBox != null) {
      myLogExpressionComboBox.setEnabled(myLogExpressionCheckBox.isSelected());
    }
  }

  @Override
  void loadProperties() {
    myLogMessageCheckBox.setSelected(myBreakpoint.isLogMessage());

    if (myBreakpoint instanceof XLineBreakpoint) {
      myTemporaryCheckBox.setSelected(((XLineBreakpoint)myBreakpoint).isTemporary());
    }

    if (myLogExpressionComboBox != null) {
      XExpression logExpression = myBreakpoint.getLogExpressionObjectInt();
      myLogExpressionComboBox.setExpression(logExpression);
      myLogExpressionCheckBox.setSelected(myBreakpoint.isLogExpressionEnabled() && logExpression != null);
    }
    onCheckboxChanged();
  }

  @Override
  void saveProperties() {
    myBreakpoint.setLogMessage(myLogMessageCheckBox.isSelected());

    if (myBreakpoint instanceof XLineBreakpoint) {
      ((XLineBreakpoint)myBreakpoint).setTemporary(myTemporaryCheckBox.isSelected());
    }

    if (myLogExpressionComboBox != null) {
      XExpression expression = myLogExpressionComboBox.getExpression();
      XExpression logExpression = !XDebuggerUtil.getInstance().isEmptyExpression(expression) ? expression : null;
      myBreakpoint.setLogExpressionEnabled(logExpression == null || myLogExpressionCheckBox.isSelected());
      myBreakpoint.setLogExpressionObject(logExpression);
      myLogExpressionComboBox.saveTextInHistory();
    }
  }

  JComponent getDefaultFocusComponent() {
    if (myLogExpressionComboBox != null && myLogExpressionComboBox.getComboBox().isEnabled()) {
      return myLogExpressionComboBox.getEditorComponent();
    }
    return null;
  }

  public void dispose() {
  }

  public void hide() {
    myContentPane.setVisible(false);
  }
}
