/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.gwt.client.ui.tree;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.client.Event;
import consulo.web.gwt.client.ui.DefaultCellTreeResources;
import consulo.web.gwt.shared.ui.state.tree.TreeState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 19-Jun-16
 */
public class GwtTreeImpl extends CellTree {
  private Runnable myOnShow;

  public GwtTreeImpl() {
    super(new GwtTreeModel(), null, GWT.<CellTree.Resources>create(DefaultCellTreeResources.class), new CellTreeMessages() {
      @Override
      public String showMore() {
        // should not never called - due page size is max
        return "SHOW_MORE";
      }

      @Override
      public String emptyTree() {
        return "loading...";
      }
    }, Integer.MAX_VALUE);

    sinkEvents(Event.ONCONTEXTMENU);

    getTreeViewModel().init(this);
  }

  public void setChildrenOpenHandler(@Nonnull Consumer<TreeState.TreeNodeState> handler) {
    getTreeViewModel().setChildrenOpenHandler(handler);
  }

  public void setDoubleClickHandler(@Nonnull Consumer<TreeState.TreeNodeState> handler) {
    getTreeViewModel().setDoubleClickHandler(handler);
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    if(myOnShow != null) {
      myOnShow.run();
    }
  }

  public void setOnShow(Runnable onShow) {
    myOnShow = onShow;
  }

  @Override
  public GwtTreeModel getTreeViewModel() {
    return (GwtTreeModel)super.getTreeViewModel();
  }

  public void expand(String nodeId) {
    TreeNode node = findNode(getRootTreeNode(), nodeId);
    if (node == null) {
      return;
    }

    int index = node.getIndex();
    node.getParent().setChildOpen(index, !node.getParent().isChildOpen(index));
  }

  @Nullable
  private TreeNode findNode(TreeNode node, String nodeId) {
    int childCount = node.getChildCount();
    for (int i = 0; i < childCount; i++) {
      TreeNode child = node.getChild(i);

      TreeState.TreeNodeState nodeState = (TreeState.TreeNodeState)child.getValue();
      if (Objects.equals(nodeState.myId, nodeId)) {
        return child;
      }

      TreeNode in = findNode(child, nodeId);
      if (in != null) {
        return in;
      }
    }

    return null;
  }

  public void handleChanges(List<TreeState.TreeChange> changes) {
    for (TreeState.TreeChange change : changes) {
      switch (change.myType) {
        case ADD:
          //todo [vistall] add
          break;
        case REMOVE:
          //todo [vistall] remove
          break;
        case SET:
          GwtTreeNode info = getTreeViewModel().getNodes().get(change.myId);
          if (info == null) {
            continue;
          }

          info.setItems(change.myNodes);
          break;
      }
    }
  }
}
