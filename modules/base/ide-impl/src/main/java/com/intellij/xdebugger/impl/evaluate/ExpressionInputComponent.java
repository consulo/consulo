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
package com.intellij.xdebugger.impl.evaluate;

import consulo.language.editor.completion.lookup.LookupManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.keymap.KeymapUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.FixedSizeButton;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBLabel;
import com.intellij.ui.popup.list.ListPopupImpl;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.ui.XDebuggerEditorBase;
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionEditor;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.disposer.Disposable;
import consulo.platform.base.icon.PlatformIconGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author nik
 */
public class ExpressionInputComponent extends EvaluationInputComponent {
  private final XDebuggerExpressionEditor myExpressionEditor;
  private final JPanel myMainPanel;

  public ExpressionInputComponent(final @Nonnull Project project, @Nonnull XDebuggerEditorsProvider editorsProvider, final @Nullable XSourcePosition sourcePosition,
                                  @Nullable XExpression expression, Disposable parentDisposable) {
    super(XDebuggerBundle.message("xdebugger.dialog.title.evaluate.expression"));
    myMainPanel = new JPanel(new BorderLayout());
    //myMainPanel.add(new JLabel(XDebuggerBundle.message("xdebugger.evaluate.label.expression")), BorderLayout.WEST);
    myExpressionEditor = new XDebuggerExpressionEditor(project, editorsProvider, "evaluateExpression", sourcePosition,
                                                       expression != null ? expression : XExpressionImpl.EMPTY_EXPRESSION, false, true, false);
    myMainPanel.add(myExpressionEditor.getComponent(), BorderLayout.CENTER);
    JButton historyButton = new FixedSizeButton(myExpressionEditor.getComponent());
    historyButton.setIcon(TargetAWT.to(PlatformIconGroup.vcsHistory()));
    historyButton.setToolTipText(XDebuggerBundle.message("xdebugger.evaluate.history.hint"));
    historyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showHistory();
      }
    });
    myMainPanel.add(historyButton, BorderLayout.EAST);
    final JBLabel help = new JBLabel(XDebuggerBundle.message("xdebugger.evaluate.addtowatches.hint",
                                                             KeymapUtil.getKeystrokeText(XDebuggerEvaluationDialog.ADD_WATCH_KEYSTROKE)),
                                     SwingConstants.RIGHT);
    help.setBorder(JBUI.Borders.empty(2, 0, 6, 0));
    help.setComponentStyle(UIUtil.ComponentStyle.SMALL);
    help.setFontColor(UIUtil.FontColor.BRIGHTER);
    myMainPanel.add(help, BorderLayout.SOUTH);
    if (expression != null) {
      myExpressionEditor.setExpression(expression);
    }
    myExpressionEditor.selectAll();

    new AnAction("XEvaluateDialog.ShowHistory") {
      @Override
      public void actionPerformed(AnActionEvent e) {
        showHistory();
      }

      @Override
      public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(LookupManager.getActiveLookup(myExpressionEditor.getEditor()) == null);
      }
    }.registerCustomShortcutSet(CustomShortcutSet.fromString("DOWN"), myMainPanel, parentDisposable);
  }

  private void showHistory() {
    List<XExpression> expressions = myExpressionEditor.getRecentExpressions();
    if (!expressions.isEmpty()) {
      ListPopupImpl popup = new ListPopupImpl(new BaseListPopupStep<XExpression>(null, expressions) {
        @Override
        public PopupStep onChosen(XExpression selectedValue, boolean finalChoice) {
          myExpressionEditor.setExpression(selectedValue);
          myExpressionEditor.requestFocusInEditor();
          return FINAL_CHOICE;
        }
      }) {
        @Override
        protected ListCellRenderer getListElementRenderer() {
          return new ColoredListCellRenderer<XExpression>() {
            @Override
            protected void customizeCellRenderer(@Nonnull JList list, XExpression value, int index, boolean selected, boolean hasFocus) {
              append(value.getExpression());
            }
          };
        }
      };
      popup.getList().setFont(EditorUtil.getEditorFont());
      popup.showUnderneathOf(myExpressionEditor.getEditorComponent());
    }
  }

  @Override
  public void addComponent(JPanel contentPanel, JPanel resultPanel) {
    contentPanel.add(resultPanel, BorderLayout.CENTER);
    contentPanel.add(myMainPanel, BorderLayout.NORTH);
  }

  @Nonnull
  protected XDebuggerEditorBase getInputEditor() {
    return myExpressionEditor;
  }
}
