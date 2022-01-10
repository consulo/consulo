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
package com.intellij.xdebugger.impl.frame.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.frame.XWatchesView;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree;
import com.intellij.xdebugger.impl.ui.tree.nodes.WatchNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.XDebuggerTreeNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import javax.annotation.Nonnull;
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
          final XExpression watchExpression = expr != null ? expr : XExpressionImpl.fromText(node.getName());
          if (watchExpression != null) {
            DebuggerUIUtil.invokeLater(new Runnable() {
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
