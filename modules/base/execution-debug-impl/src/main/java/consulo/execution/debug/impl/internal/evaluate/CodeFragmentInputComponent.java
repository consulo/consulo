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
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBSplitter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author nik
 */
public class CodeFragmentInputComponent extends EvaluationInputComponent {
  private final XDebuggerExpressionEditor myMultilineEditor;
  private final JPanel myMainPanel;
  private final String mySplitterProportionKey;

  public CodeFragmentInputComponent(final @Nonnull Project project,
                                    @Nonnull XDebuggerEditorsProvider editorsProvider,
                                    final @Nullable XSourcePosition sourcePosition,
                                    @Nullable XExpression statements,
                                    String splitterProportionKey,
                                    Disposable parentDisposable) {
    super(XDebuggerBundle.message("dialog.title.evaluate.code.fragment"));
    myMultilineEditor = new XDebuggerExpressionEditor(project, editorsProvider, "evaluateCodeFragment", sourcePosition,
                                                      statements != null ? statements : XExpression.EMPTY_CODE_FRAGMENT, true, true, false);
    myMainPanel = new JPanel(new BorderLayout());
    JPanel editorPanel = new JPanel(new BorderLayout());
    editorPanel.add(myMultilineEditor.getComponent(), BorderLayout.CENTER);
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new HistoryNavigationAction(false, IdeActions.ACTION_PREVIOUS_OCCURENCE, parentDisposable));
    group.add(new HistoryNavigationAction(true, IdeActions.ACTION_NEXT_OCCURENCE, parentDisposable));
    editorPanel.add(ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, false).getComponent(), BorderLayout.EAST);
    //myMainPanel.add(new JLabel(XDebuggerBundle.message("xdebugger.label.text.code.fragment")), BorderLayout.NORTH);
    myMainPanel.add(editorPanel, BorderLayout.CENTER);
    if (statements != null) {
      myMultilineEditor.setExpression(statements);
    }
    mySplitterProportionKey = splitterProportionKey;
  }

  @Override
  @Nonnull
  public XDebuggerEditorBase getInputEditor() {
    return myMultilineEditor;
  }

  public JPanel getMainComponent() {
    return myMainPanel;
  }

  @Override
  public void addComponent(JPanel contentPanel, JPanel resultPanel) {
    final JBSplitter splitter = new JBSplitter(true, 0.3f, 0.2f, 0.7f);
    splitter.setSplitterProportionKey(mySplitterProportionKey);
    contentPanel.add(splitter, BorderLayout.CENTER);
    splitter.setFirstComponent(myMainPanel);
    splitter.setSecondComponent(resultPanel);
  }

  private class HistoryNavigationAction extends AnAction {
    private final boolean myForward;

    public HistoryNavigationAction(boolean forward, String actionId, Disposable parentDisposable) {
      myForward = forward;
      final AnAction action = ActionManager.getInstance().getAction(actionId);
      copyFrom(action);
      registerCustomShortcutSet(action.getShortcutSet(), myMainPanel, parentDisposable);
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(myForward ? myMultilineEditor.canGoForward() : myMultilineEditor.canGoBackward());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      if (myForward) {
        myMultilineEditor.goForward();
      }
      else {
        myMultilineEditor.goBackward();
      }
    }
  }
}
