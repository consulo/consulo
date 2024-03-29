/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.xdebugger.impl.ui.tree.actions;

import consulo.ui.ex.action.AnActionEvent;
import consulo.execution.debug.frame.XValue;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XDebuggerTree;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XInspectDialog;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class XInspectAction extends XDebuggerTreeActionBase {
  @Override
  protected void perform(XValueNodeImpl node, @Nonnull final String nodeName, AnActionEvent e) {
    XDebuggerTree tree = node.getTree();
    XValue value = node.getValueContainer();
    XInspectDialog dialog = new XInspectDialog(tree.getProject(), tree.getEditorsProvider(), tree.getSourcePosition(), nodeName, value,
                                               tree.getValueMarkers());
    dialog.show();
  }
}
