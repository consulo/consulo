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
package consulo.ide.impl.idea.xdebugger.impl.ui.tree;

import consulo.codeEditor.Editor;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.frame.XValueModifier;
import consulo.execution.debug.frame.presentation.XValuePresentation;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.execution.debug.ui.XValuePresentationUtil;
import consulo.ide.impl.idea.ui.AppUIUtil;
import consulo.ide.impl.idea.xdebugger.impl.ui.DebuggerUIUtil;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import consulo.language.editor.hint.HintManager;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.SimpleColoredComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author nik
 */
public class SetValueInplaceEditor extends XDebuggerTreeInplaceEditor {
  private final JPanel myEditorPanel;
  private final XValueModifier myModifier;
  private final XValueNodeImpl myValueNode;
  private final int myNameOffset;

  private SetValueInplaceEditor(final XValueNodeImpl node, @Nonnull final String nodeName) {
    super(node, "setValue");
    myValueNode = node;
    myModifier = myValueNode.getValueContainer().getModifier();

    SimpleColoredComponent nameLabel = new SimpleColoredComponent();
    nameLabel.getIpad().right = 0;
    nameLabel.getIpad().left = 0;
    nameLabel.setIcon(myNode.getIcon());
    nameLabel.append(nodeName, XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES);
    XValuePresentation presentation = node.getValuePresentation();
    if (presentation != null) {
      XValuePresentationUtil.appendSeparator(nameLabel, presentation.getSeparator());
    }
    myNameOffset = nameLabel.getPreferredSize().width;
    myEditorPanel = JBUI.Panels.simplePanel(myExpressionEditor.getComponent());
  }

  @Nullable
  @Override
  protected Rectangle getEditorBounds() {
    Rectangle bounds = super.getEditorBounds();
    if (bounds == null) {
      return null;
    }
    bounds.x += myNameOffset;
    bounds.width -= myNameOffset;
    return bounds;
  }

  public static void show(final XValueNodeImpl node, @Nonnull final String nodeName) {
    final SetValueInplaceEditor editor = new SetValueInplaceEditor(node, nodeName);

    if (editor.myModifier != null) {
      editor.myModifier.calculateInitialValueEditorText(initialValue -> AppUIUtil.invokeOnEdt(() -> {
        if (editor.getTree().isShowing()) {
          editor.show(initialValue);
        }
      }));
    }
    else {
      editor.show(null);
    }
  }

  private void show(String initialValue) {
    myExpressionEditor.setExpression(XExpression.fromText(initialValue));
    myExpressionEditor.selectAll();

    show();
  }

  @Override
  protected JComponent createInplaceEditorComponent() {
    return myEditorPanel;
  }

  @Override
  public void doOKAction() {
    if (myModifier == null) return;

    DebuggerUIUtil.setTreeNodeValue(myValueNode, getExpression().getExpression(), errorMessage -> {
      Editor editor = getEditor();
      if (editor != null) {
        HintManager.getInstance().showErrorHint(editor, errorMessage);
      }
      else {
        Messages.showErrorDialog(myTree, errorMessage);
      }
    });

    super.doOKAction();
  }
}
