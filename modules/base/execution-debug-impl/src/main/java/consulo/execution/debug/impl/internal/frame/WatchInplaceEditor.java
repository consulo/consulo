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
package consulo.execution.debug.impl.internal.frame;

import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTreeInplaceEditor;
import consulo.execution.debug.impl.internal.ui.tree.node.WatchNode;
import consulo.execution.debug.impl.internal.ui.tree.node.WatchesRootNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XDebuggerTreeNode;
import consulo.ui.ex.awt.tree.TreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class WatchInplaceEditor extends XDebuggerTreeInplaceEditor {
  private final WatchesRootNode myRootNode;
  private final XWatchesView myWatchesView;
  private final WatchNode myOldNode;

  public WatchInplaceEditor(@Nonnull WatchesRootNode rootNode,
                            XWatchesView watchesView,
                            WatchNode node,
                            @Nullable WatchNode oldNode) {
    super((XDebuggerTreeNode)node, "watch");
    myRootNode = rootNode;
    myWatchesView = watchesView;
    myOldNode = oldNode;
    myExpressionEditor.setExpression(oldNode != null ? oldNode.getExpression() : null);
  }

  @Override
  public void cancelEditing() {
    if (!isShown()) return;
    super.cancelEditing();
    int index = myRootNode.getIndex(myNode);
    if (myOldNode == null && index != -1) {
      myRootNode.removeChildNode(myNode);
    }
    TreeUtil.selectNode(myTree, myNode);
  }

  @Override
  public void doOKAction() {
    XExpression expression = getExpression();
    super.doOKAction();
    int index = myRootNode.removeChildNode(myNode);
    if (!XDebuggerUtil.getInstance().isEmptyExpression(expression) && index != -1) {
      myWatchesView.addWatchExpression(expression, index, false);
    }
    TreeUtil.selectNode(myTree, myNode);
  }
}
