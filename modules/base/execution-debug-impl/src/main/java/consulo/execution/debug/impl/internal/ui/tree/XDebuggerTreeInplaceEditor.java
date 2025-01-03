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
package consulo.execution.debug.impl.internal.ui.tree;

import consulo.codeEditor.Editor;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.event.XDebugSessionListener;
import consulo.execution.debug.impl.internal.frame.XDebugView;
import consulo.execution.debug.impl.internal.ui.XDebuggerExpressionComboBox;
import consulo.execution.debug.impl.internal.ui.tree.node.XDebuggerTreeNode;
import consulo.project.Project;
import consulo.project.ui.util.AppUIUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.tree.TreePath;

/**
 * @author nik
 */
public abstract class XDebuggerTreeInplaceEditor extends TreeInplaceEditor {
  protected final XDebuggerTreeNode myNode;
  protected final XDebuggerExpressionComboBox myExpressionEditor;
  protected final XDebuggerTree myTree;

  public XDebuggerTreeInplaceEditor(final XDebuggerTreeNode node, @NonNls final String historyId) {
    myNode = node;
    myTree = myNode.getTree();
    myExpressionEditor = new XDebuggerExpressionComboBox(myTree.getProject(), myTree.getEditorsProvider(), historyId, myTree.getSourcePosition(), false);
  }

  @Override
  protected JComponent createInplaceEditorComponent() {
    return myExpressionEditor.getComponent();
  }

  @Override
  protected void onHidden() {
    final ComboPopup popup = myExpressionEditor.getComboBox().getPopup();
    if (popup != null && popup.isVisible()) {
      popup.hide();
    }
  }

  @Override
  protected void doPopupOKAction() {
    ComboPopup popup = myExpressionEditor.getComboBox().getPopup();
    if (popup != null && popup.isVisible()) {
      Object value = popup.getList().getSelectedValue();
      if (value != null) {
        myExpressionEditor.setExpression((XExpression)value);
      }
    }
    doOKAction();
  }

  @Override
  public void doOKAction() {
    myExpressionEditor.saveTextInHistory();
    super.doOKAction();
  }

  @Override
  protected void onShown() {
    XDebugSession session = XDebugView.getSession(myTree);
    if (session != null) {
      session.addSessionListener(new XDebugSessionListener() {
        @Override
        public void sessionPaused() {
          cancel();
        }

        @Override
        public void sessionResumed() {
          cancel();
        }

        @Override
        public void sessionStopped() {
          cancel();
        }

        @Override
        public void stackFrameChanged() {
          cancel();
        }

        @Override
        public void beforeSessionResume() {
          cancel();
        }

        private void cancel() {
          AppUIUtil.invokeOnEdt(XDebuggerTreeInplaceEditor.this::cancelEditing);
        }
      }, myDisposable);
    }
  }

  protected XExpression getExpression() {
    return myExpressionEditor.getExpression();
  }

  @Override
  protected JComponent getPreferredFocusedComponent() {
    return myExpressionEditor.getPreferredFocusedComponent();
  }

  @Override
  public Editor getEditor() {
    return myExpressionEditor.getEditor();
  }

  @Override
  public JComponent getEditorComponent() {
    return myExpressionEditor.getEditorComponent();
  }

  @Override
  protected TreePath getNodePath() {
    return myNode.getPath();
  }

  @Override
  protected XDebuggerTree getTree() {
    return myTree;
  }

  @Override
  protected Project getProject() {
    return myNode.getTree().getProject();
  }
}
