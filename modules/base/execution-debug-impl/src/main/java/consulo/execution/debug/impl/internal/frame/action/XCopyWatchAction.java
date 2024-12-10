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
package consulo.execution.debug.impl.internal.frame.action;

import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.impl.internal.frame.XWatchesView;
import consulo.execution.debug.impl.internal.ui.DebuggerUIImplUtil;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.node.WatchNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XDebuggerTreeNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author egor
 */
public class XCopyWatchAction extends XWatchesTreeActionBase {

  @Override
  protected boolean isEnabled(@Nonnull AnActionEvent e, @Nonnull XDebuggerTree tree) {
    return !getSelectedNodes(tree, XValueNodeImpl.class).isEmpty();
  }

  @Override
  protected void perform(@Nonnull AnActionEvent e, @Nonnull XDebuggerTree tree, @Nonnull final XWatchesView watchesView) {
    final XDebuggerTreeNode root = tree.getRoot();
    for (final XValueNodeImpl node : getSelectedNodes(tree, XValueNodeImpl.class)) {
      node.getValueContainer().calculateEvaluationExpression().doWhenDone(new Consumer<XExpression>() {
        @Override
        public void accept(XExpression expr) {
          final XExpression watchExpression = expr != null ? expr : XExpression.fromText(node.getName());
          if (watchExpression != null) {
            DebuggerUIImplUtil.invokeLater(new Runnable() {
              @Override
              public void run() {
                watchesView.addWatchExpression(watchExpression, node instanceof WatchNode ? root.getIndex(node) + 1 : -1, true);
              }
            });
          }
        }
      });
    }
  }
}
