/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui.tree.action;

import consulo.execution.debug.impl.internal.ui.tree.SetValueInplaceEditor;
import consulo.execution.debug.impl.internal.ui.tree.node.WatchNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class XSetValueAction extends XDebuggerTreeActionBase {
  @Override
  public void update(final AnActionEvent e) {
    super.update(e);
    XValueNodeImpl node = getSelectedNode(e.getDataContext());
    Presentation presentation = e.getPresentation();
    if (node instanceof WatchNode) {
      presentation.setVisible(false);
      presentation.setEnabled(false);
    }
    else {
      presentation.setVisible(true);
    }
  }

  @Override
  protected boolean isEnabled(@Nonnull XValueNodeImpl node, @Nonnull AnActionEvent e) {
    return super.isEnabled(node, e) && node.getValueContainer().getModifier() != null;
  }

  @Override
  protected void perform(final XValueNodeImpl node, @Nonnull final String nodeName, final AnActionEvent e) {
    SetValueInplaceEditor.show(node, nodeName);
  }
}
