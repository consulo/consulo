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
package consulo.execution.debug.impl.internal.frame.action;

import consulo.execution.debug.impl.internal.frame.XWatchesView;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.node.WatchNodeImpl;
import consulo.execution.debug.impl.internal.ui.tree.node.WatchesRootNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XDebuggerTreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author nik
 */
public class XEditWatchAction extends XWatchesTreeActionBase {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    XDebuggerTree tree = XDebuggerTree.getTree(e);
    e.getPresentation().setVisible(tree != null && getSelectedNodes(tree, WatchNodeImpl.class).size() == 1);
    super.update(e);
  }

  @Override
  protected void perform(@Nonnull AnActionEvent e, @Nonnull XDebuggerTree tree, @Nonnull XWatchesView watchesView) {
    List<? extends WatchNodeImpl> watchNodes = getSelectedNodes(tree, WatchNodeImpl.class);
    if (watchNodes.size() != 1) return;

    WatchNodeImpl node = watchNodes.get(0);
    XDebuggerTreeNode root = tree.getRoot();
    if (root instanceof WatchesRootNode) {
      ((WatchesRootNode)root).editWatch(node);
    }
  }
}
