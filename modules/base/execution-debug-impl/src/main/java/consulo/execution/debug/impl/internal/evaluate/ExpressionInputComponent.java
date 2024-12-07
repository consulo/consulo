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
package consulo.execution.debug.impl.internal.evaluate;

import consulo.disposer.Disposable;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.impl.internal.ui.XDebuggerEditorBase;
import consulo.execution.debug.impl.internal.ui.XDebuggerExpressionEditor;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.ui.awt.AWTLanguageEditorUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.popup.AWTListPopup;
import consulo.ui.ex.awt.popup.AWTPopupFactory;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author nik
 */
public class ExpressionInputComponent extends EvaluationInputComponent {
    private final XDebuggerExpressionEditor myExpressionEditor;
    private final JPanel myMainPanel;
    @Nonnull
    private final Project myProject;

    public ExpressionInputComponent(
        final @Nonnull Project project,
        @Nonnull XDebuggerEditorsProvider editorsProvider,
        final @Nullable XSourcePosition sourcePosition,
        @Nullable XExpression expression,
        Disposable parentDisposable
    ) {
        super(XDebuggerLocalize.xdebuggerDialogTitleEvaluateExpression().get());
        myProject = project;
        myMainPanel = new JPanel(new BorderLayout());
        myExpressionEditor = new XDebuggerExpressionEditor(
            project,
            editorsProvider,
            "evaluateExpression",
            sourcePosition,
            expression != null ? expression : XExpression.EMPTY_EXPRESSION,
            false,
            true,
            false
        );
        myMainPanel.add(myExpressionEditor.getComponent(), BorderLayout.CENTER);
        JButton historyButton = new FixedSizeButton(myExpressionEditor.getComponent());
        historyButton.setIcon(TargetAWT.to(PlatformIconGroup.vcsHistory()));
        historyButton.setToolTipText(XDebuggerLocalize.xdebuggerEvaluateHistoryHint().get());
        historyButton.addActionListener(e -> showHistory());
        myMainPanel.add(historyButton, BorderLayout.EAST);
        final JBLabel help = new JBLabel(
            XDebuggerBundle.message(
                "xdebugger.evaluate.addtowatches.hint",
                ShortcutUtil.getKeystrokeText(XDebuggerEvaluationDialog.ADD_WATCH_KEYSTROKE)
            ),
            SwingConstants.RIGHT
        );
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
            BaseListPopupStep<XExpression> step = new BaseListPopupStep<XExpression>(null, expressions) {
                @Override
                public PopupStep onChosen(XExpression selectedValue, boolean finalChoice) {
                    myExpressionEditor.setExpression(selectedValue);
                    myExpressionEditor.requestFocusInEditor();
                    return FINAL_CHOICE;
                }
            };

            AWTPopupFactory awtPopupFactory = (AWTPopupFactory) JBPopupFactory.getInstance();

            AWTListPopup popup = awtPopupFactory.createListPopup(myProject, step, p -> {
                return new ColoredListCellRenderer<XExpression>() {
                    @Override
                    protected void customizeCellRenderer(@Nonnull JList list, XExpression value, int index, boolean selected, boolean hasFocus) {
                        append(value.getExpression());
                    }
                };
            });

            popup.getList().setFont(AWTLanguageEditorUtil.getEditorFont());
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
