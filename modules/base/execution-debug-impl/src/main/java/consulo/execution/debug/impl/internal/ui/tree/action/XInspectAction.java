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
package consulo.execution.debug.impl.internal.ui.tree.action;

import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.XInspectDialog;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.ui.ex.action.AnActionEvent;
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
